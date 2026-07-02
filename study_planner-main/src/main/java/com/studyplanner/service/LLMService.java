package com.studyplanner.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.studyplanner.dto.PlanGenerateRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMService {

    @Value("${llm.api.base-url:}")
    private String defaultBaseUrl;

    @Value("${llm.api.api-key:}")
    private String defaultApiKey;

    @Value("${llm.api.model:qwen-plus}")
    private String defaultModel;

    @Value("${llm.api.max-tokens:4096}")
    private Integer maxTokens;

    @Value("${llm.api.temperature:0.7}")
    private Double temperature;

    @Value("${llm.api.mock-mode:false}")
    private Boolean mockMode;

    @Value("${llm.api.available-models:qwen-plus,qwen-turbo,qwen-max,qwen-long}")
    private String availableModelsConfig;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public List<String> getAvailableModels() {
        return Arrays.asList(availableModelsConfig.split(","));
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public JSONObject analyzeGoal(String goal, String level) {
        if (Boolean.TRUE.equals(mockMode)) {
            return buildFallbackGoalAnalysis(goal, level);
        }

        String prompt = String.format("""
                你是学习规划分析助手。请根据学习目标输出结构化分析。
                学习目标：%s
                当前基础：%s

                只返回合法 JSON，不要 Markdown，不要解释。字段必须为：
                {
                  "keySkills": ["技能1", "技能2", "技能3"],
                  "recommendedMode": "relaxed",
                  "completionRate": 82,
                  "tip": "一句简短建议"
                }

                规则：
                1. keySkills 是从目标中抽取的 2~4 个核心技术/知识点，用数组表示。
                2. recommendedMode 取值 "relaxed"（轻松，每天1h）、"standard"（标准，每天2h）或 "sprint"（冲刺，每天4h），根据目标和剩余天数判断。
                3. tip 是一句简短的学习建议。
                """, goal, hasText(level) ? level : "未选择");

        try {
            return normalizeGoalAnalysis(parseObject(chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, 1000)), goal, level);
        } catch (RuntimeException e) {
            return buildFallbackGoalAnalysis(goal, level);
        }
    }

    public JSONObject optimizeGoal(String goal, String level, Integer totalDays, Double dailyHours) {
        if (Boolean.TRUE.equals(mockMode)) {
            return buildFallbackOptimizedGoal(goal, level, totalDays, dailyHours);
        }

        String prompt = String.format("""
                你是毕业设计学习目标续写助手。用户已经输入了一段口语化的学习目标，请在【不改动、不重写用户原文】的前提下，在原文后面续写一段补充内容，让目标更清晰、可衡量、颗粒度更细。
                用户原始目标：%s
                当前基础：%s
                计划天数：%s
                每日学习时间：%s

                只返回合法 JSON，不要 Markdown，不要解释。字段必须为：
                {
                  "goal": "用户原文 + 续写补充，整体不超过90字"
                }

                续写标准：
                1. 【最重要】返回的 goal 必须以用户原始目标原文【一字不差】地开头，只能在末尾追加补充，绝不能改写、替换、删减或重复用户原文中已表达的主题。
                2. 续写部分要与原文自然衔接：如果原文末尾已有标点（逗号/句号等），直接接内容；如果没有标点，用一个逗号衔接，不要出现连续两个标点。
                3. 补充具体工具/库/框架，例如 OpenCV、PyTorch、TensorFlow、CNN、YOLO、Spring Boot、MySQL、Vue3 等；如果原目标没有明显技术栈，补充最贴切的通用技术或方法。
                4. 使用具体动作词，例如“搭建、部署、复现、验证、训练、调优、联调、实现、评估”。
                5. 写清具体产出，例如“高精度模型、完整应用 Demo、后端接口模块、数据分析报告、可部署系统、阶段测试结果”。
                6. 避免空泛表达，例如“掌握基础、完成项目、能够独立完成一个可展示的实战项目”。

                示例：
                用户原始目标：我想学习java
                正确输出：{"goal": "我想学习java，掌握核心语法并基于 Spring Boot、MySQL 实现一个可运行的后端接口模块"}
                错误输出（改写了原文，禁止）：{"goal": "学习Java编程语言，掌握核心语法并实现后端接口模块"}
                """, goal, hasText(level) ? level : "未选择",
                totalDays != null ? totalDays + "天" : "未填写",
                dailyHours != null ? dailyHours + "小时" : "未填写");

        try {
            JSONObject result = parseObject(chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, 1000));
            String optimized = result.getString("goal");
            if (!hasText(optimized)) {
                return buildFallbackOptimizedGoal(goal, level, totalDays, dailyHours);
            }
            // 兜底：大模型若没保留原文开头，强制规整为「原文 + 补充」，避免前端拼接出重复
            result.put("goal", ensureGoalPrefixClean(goal, optimized));
            return result;
        } catch (RuntimeException e) {
            return buildFallbackOptimizedGoal(goal, level, totalDays, dailyHours);
        }
    }

    /**
     * 保证优化结果以用户原文开头（续写而非改写）。
     * 若大模型返回的是改写全文（不以原文开头），则只把补充部分接到原文后，避免重复与双标点。
     */
    private String ensureGoalPrefixClean(String goal, String optimized) {
        String base = goal == null ? "" : goal.trim();
        String opt = optimized == null ? "" : optimized.trim();
        String trailingPunct = "[\\uFF0C,\\u3002.!\\uFF01?\\uFF1F\\uFF1B;\\u3001:\\s]+$";
        String leadingPunct = "^[\\uFF0C,\\u3002.!\\uFF01?\\uFF1F\\uFF1B;\\u3001:\\s]+";
        String endingPunct = ".*[\\uFF0C,\\u3002.!\\uFF01?\\uFF1F\\uFF1B;\\u3001:]$";

        if (base.isEmpty()) {
            return opt;
        }
        if (opt.isEmpty() || opt.equals(base)) {
            return base;
        }
        if (opt.startsWith(base)) {
            return opt;
        }

        String baseCore = base.replaceAll(trailingPunct, "");
        if (!baseCore.isEmpty() && opt.startsWith(baseCore)) {
            String suffix = opt.substring(baseCore.length()).replaceFirst(leadingPunct, "");
            if (suffix.isEmpty()) {
                return base;
            }
            return base.matches(endingPunct) ? base + " " + suffix : opt;
        }

        String suffix = opt.replaceFirst(leadingPunct, "");
        return base + (base.matches(endingPunct) ? " " : "\uFF0C") + suffix;
    }

    private String ensureGoalPrefix(String goal, String optimized) {
        String base = goal == null ? "" : goal.trim();
        String opt = optimized == null ? "" : optimized.trim();
        String trailingPunct = "[，,。.!！?？；;、:\\s]+$";
        String leadingPunct = "^[，,。.!！?？；;、:\\s]+";
        if (base.isEmpty()) {
            return opt;
        }
        if (opt.isEmpty()) {
            return base;
        }
        // 已正确以原文开头：直接用
        if (opt.startsWith(base)) {
            return opt;
        }
        // 去掉原文末尾标点后再比一次，命中则用模型结果（它只是补了个更合适的衔接标点）
        String baseCore = base.replaceAll("[，,。.!！?？;；:：]+$", "");
        if (!baseCore.isEmpty() && opt.startsWith(baseCore)) {
            String suffix = opt.substring(baseCore.length()).replaceFirst(leadingPunct, "");
            if (suffix.isEmpty()) {
                return base;
            }
            return base.matches(".*[，,。.!！?？；;、:]$") ? base + " " + suffix : opt;
        }
        // 模型改写了全文：把它整段当补充接到原文后，并规整衔接标点，避免双标点
        String connector = base.matches(".*[，,。.!！?？;；:：]$") ? "" : "，";
        String suffix = opt.replaceFirst("^[，,。.!！?？;；:：\\s]+", "");
        return base + connector + suffix;
    }

    public JSONArray generateRoadmap(String goal, String level, Integer totalDays, Double dailyHours,
                                     String studyMode, String studyPreference, Map<String, Object> goalAnalysis) {
        if (Boolean.TRUE.equals(mockMode)) {
            return buildFallbackRoadmap(goal, totalDays, studyMode, studyPreference);
        }

        int days = totalDays != null && totalDays > 0 ? totalDays : 30;
        int stageCount = calculateRoadmapStageCount(days);
        String modeInstruction = describeStudyMode(studyMode);
        String preferenceInstruction = describeStudyPreference(studyPreference);
        String prompt = String.format("""
                你是学习路线规划助手。请根据目标和计划天数生成学习路线预览。
                学习目标：%s
                当前基础：%s
                计划天数：%d
                每日学习时间：%.1f小时
                学习模式：%s
                学习偏好：%s
                目标分析：%s

                只返回合法 JSON 数组，不要 Markdown，不要解释。数组项字段必须为：
                [
                  { "label": "第1天", "dayStart": 1, "dayEnd": 1, "title": "阶段标题", "desc": "阶段学习内容，20字以内" }
                ]

                要求：
                1. 必须严格覆盖第1天到第%d天，不要超过计划天数。
                2. 必须生成 %d 个阶段；计划天数少于或等于14天时按天生成。
                3. label 使用“第N天”或“第N-M天”，不要使用“第N周”。
                4. 路线要贴合学习目标，不要泛泛而谈。
                5. 标题短，描述具体。
                6. 阶段标题和描述必须强烈体现「学习偏好」：
                   - 偏理论：标题/描述围绕概念框架、原理推导、知识体系，几乎不出现“刷题/做项目”。
                   - 偏实战：标题/描述围绕动手练习、即学即用、编码训练。
                   - 偏考试：标题/描述围绕考点梳理、题型训练、真题模考、错题复盘。
                   - 偏项目：标题/描述围绕需求拆解、模块开发、集成联调、部署交付、作品打磨。
                   同一目标在不同偏好下，阶段标题应当明显不同，而不是只改几个字。
                7. 同时体现学习模式的节奏差异（轻松更松、冲刺更紧凑）。
                """, goal, hasText(level) ? level : "未选择", days,
                dailyHours != null ? dailyHours : 2.0,
                modeInstruction,
                preferenceInstruction,
                goalAnalysis != null ? goalAnalysis.toString() : "{}", days, stageCount);

        try {
            return normalizeRoadmap(parseArray(chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, 1800)), goal, days);
        } catch (RuntimeException e) {
            return buildFallbackRoadmap(goal, totalDays, studyMode, studyPreference);
        }
    }

    public String generateStudyPlan(PlanGenerateRequest request) {
        if (Boolean.TRUE.equals(mockMode)) {
            return generateMockPlan(
                    request.getGoal(),
                    request.getLevel(),
                    request.getDailyHours().doubleValue(),
                    request.getTotalDays());
        }

        String useModel = hasText(request.getModelName()) ? request.getModelName() : defaultModel;
        String prompt = buildPlanPrompt(request);
        return chat(prompt, defaultBaseUrl, defaultApiKey, useModel, getPlanMaxTokens(request.getTotalDays()));
    }

    public String generateStudyPlan(String goal, String level, double dailyHours, int totalDays, String modelName) {
        if (Boolean.TRUE.equals(mockMode)) {
            return generateMockPlan(goal, level, dailyHours, totalDays);
        }

        String useModel = hasText(modelName) ? modelName : defaultModel;
        String prompt = buildPlanPrompt(goal, level, dailyHours, totalDays);
        return chat(prompt, defaultBaseUrl, defaultApiKey, useModel, getPlanMaxTokens(totalDays));
    }

    public String generateStudyPlanWithCustomConfig(PlanGenerateRequest request) {
        validateCustomConfig(request.getCustomApiUrl(), request.getCustomApiKey(), request.getModelName());
        return chat(
                buildPlanPrompt(request),
                request.getCustomApiUrl(),
                request.getCustomApiKey(),
                request.getModelName(),
                getPlanMaxTokens(request.getTotalDays()));
    }

    public String generateStudyPlanWithCustomConfig(String goal, String level, double dailyHours, int totalDays,
                                                     String customBaseUrl, String customApiKey, String customModel) {
        validateCustomConfig(customBaseUrl, customApiKey, customModel);
        return chat(
                buildPlanPrompt(goal, level, dailyHours, totalDays),
                customBaseUrl,
                customApiKey,
                customModel,
                getPlanMaxTokens(totalDays));
    }

    public String repairStudyPlanResponse(PlanGenerateRequest request, String badResponse, String qualityReport) {
        String prompt = String.format("""
                你是学习计划 JSON 修复器。下面的大模型输出未通过系统校验，请根据原始需求修复为合法 JSON。

                原始需求：
                - 学习目标：%s
                - 当前基础：%s
                - 每日学习时间：%.1f 小时
                - 计划天数：%d 天

                校验问题：
                %s

                待修复输出：
                %s

                只返回合法 JSON，不要 Markdown，不要解释。字段必须完全匹配：
                {
                  "title": "不超过30字",
                  "summary": "不超过80字",
                  "dailyPlans": [
                    { "day": 1, "content": "具体、可执行的今日任务", "duration": %.1f }
                  ]
                }

                要求：
                1. dailyPlans 必须正好 %d 项，day 从 1 到 %d 连续递增。
                2. 每天任务要贴合学习目标，不能只写泛泛的“学习并练习”。
                3. duration 必须在 0.5 到 4 之间，默认使用用户每日学习时间。
                """,
                request.getGoal(),
                request.getLevel(),
                request.getDailyHours().doubleValue(),
                request.getTotalDays(),
                qualityReport,
                trimForPrompt(badResponse, 5000),
                request.getDailyHours().doubleValue(),
                request.getTotalDays(),
                request.getTotalDays());

        return chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, getPlanMaxTokens(request.getTotalDays()));
    }

    public String generateDraftAssistantReply(Map<String, Object> draftContext, String question) {
        if (Boolean.TRUE.equals(mockMode)) {
            return "建议先把学习目标写成可交付成果，再根据当前基础选择合适强度。若时间紧，可以优先保留核心概念、最小项目和复盘三类任务。";
        }

        String prompt = String.format("""
                你是学习计划副驾驶，正在辅助用户创建学习计划。
                请基于草稿上下文回答用户问题，必须具体、简短、可执行，不要编造用户未提供的信息。

                草稿上下文：
                %s

                用户问题：
                %s

                输出要求：
                - 直接给建议，控制在 120 字以内。
                - 如发现计划过紧或目标过泛，要明确指出并给一个调整动作。
                """, JSON.toJSONString(draftContext), question);

        return chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, 700);
    }

    public String generatePlanAssistantReply(Map<String, Object> planContext, String question) {
        if (Boolean.TRUE.equals(mockMode)) {
            return "结合当前进度，建议先完成最近一个未完成任务；如果实际用时连续高于预计时长，可以把后续任务拆小并顺延 1-2 天。";
        }

        String prompt = String.format("""
                你是学习计划副驾驶。你能看到用户的学习计划、任务、打卡、延误和用时偏差。
                请基于上下文回答用户问题，给出具体下一步，不要泛泛鼓励。

                计划上下文：
                %s

                用户问题：
                %s

                输出要求：
                - 中文回答。
                - 120 字以内。
                - 优先引用当前任务、延误、预计/实际用时等事实。
                - 如果需要调整计划，请建议用户使用“一键调整”。
                """, JSON.toJSONString(planContext), question);

        return chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, 800);
    }

    public JSONObject suggestPlanAdjustment(Map<String, Object> planContext) {
        if (Boolean.TRUE.equals(mockMode)) {
            return buildFallbackAdjustment(planContext);
        }

        String prompt = String.format("""
                你是学习计划自适应调整引擎。请根据用户计划上下文判断是否需要调整后续未完成任务。

                计划上下文：
                %s

                只返回合法 JSON，不要 Markdown，不要解释。字段必须为：
                {
                  "needAdjustment": true,
                  "confidence": 82,
                  "summary": "一句话说明为什么要调或不用调",
                  "actions": [
                    {
                      "detailId": 123,
                      "scheduledDate": "2026-06-28",
                      "duration": 2.5,
                      "content": "保留或优化后的任务内容",
                      "reason": "调整原因"
                    }
                  ],
                  "notes": ["给用户的执行提醒"]
                }

                规则：
                1. 只允许调整上下文里给出的未完成 futureTasks。
                2. 如果用户明显落后、实际用时高于预计，优先顺延和拆小任务。
                3. 如果进度正常，needAdjustment=false，actions 返回空数组。
                4. scheduledDate 使用 yyyy-MM-dd。
                5. actions 不超过 8 条。
                """, JSON.toJSONString(planContext));

        try {
            return parseObject(chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel, 1600));
        } catch (RuntimeException e) {
            return buildFallbackAdjustment(planContext);
        }
    }

    private void validateCustomConfig(String customBaseUrl, String customApiKey, String customModel) {
        if (!hasText(customBaseUrl)) {
            throw new IllegalArgumentException("API URL不能为空");
        }
        if (!hasText(customApiKey)) {
            throw new IllegalArgumentException("API Key不能为空");
        }
        if (!hasText(customModel)) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
    }

    private String generateMockPlan(String goal, String level, double dailyHours, int totalDays) {
        JSONObject plan = new JSONObject();
        plan.put("title", goal + " - 学习计划");
        plan.put("summary", "为期" + totalDays + "天的个性化学习计划，适合" + level + "学习者。");

        JSONArray dailyPlans = new JSONArray();
        for (int i = 1; i <= totalDays; i++) {
            JSONObject dayPlan = new JSONObject();
            dayPlan.put("day", i);
            dayPlan.put("content", "第" + i + "天：围绕" + goal + "完成知识学习、练习和复盘");
            dayPlan.put("duration", dailyHours);

            dailyPlans.add(dayPlan);
        }
        plan.put("dailyPlans", dailyPlans);
        return plan.toJSONString();
    }

    public String askQuestion(String question) {
        String prompt = "你是专业学习助手，请清晰回答以下问题：\n\n" + question;
        return chat(prompt, defaultBaseUrl, defaultApiKey, defaultModel);
    }

    public String chat(String baseUrl, String apiKey, String model, List<Map<String, String>> messageList) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            JSONArray messages = new JSONArray();
            for (Map<String, String> msg : messageList) {
                JSONObject message = new JSONObject();
                message.put("role", msg.get("role"));
                message.put("content", msg.get("content"));
                messages.add(message);
            }
            requestBody.put("messages", messages);

            return executeChatRequest(baseUrl, apiKey, requestBody);
        } catch (IOException e) {
            throw new RuntimeException("调用LLM API失败: " + e.getMessage(), e);
        }
    }

    public String chat(String prompt, String baseUrl, String apiKey, String model) {
        return chat(prompt, baseUrl, apiKey, model, maxTokens);
    }

    public String chat(String prompt, String baseUrl, String apiKey, String model, Integer responseMaxTokens) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("max_tokens", responseMaxTokens != null ? responseMaxTokens : maxTokens);
            requestBody.put("temperature", temperature);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);

            return executeChatRequest(baseUrl, apiKey, requestBody);
        } catch (IOException e) {
            throw new RuntimeException("调用LLM API失败: " + e.getMessage(), e);
        }
    }

    private String executeChatRequest(String baseUrl, String apiKey, JSONObject requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(buildChatCompletionsUrl(baseUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("LLM API调用失败: " + response.code() + " - " + responseBody);
            }

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            return jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }

    private int getPlanMaxTokens(int totalDays) {
        int estimatedTokens = 1200 + totalDays * 120;
        return Math.max(maxTokens, Math.min(estimatedTokens, 8192));
    }

    private String buildChatCompletionsUrl(String baseUrl) {
        if (!hasText(baseUrl)) {
            throw new IllegalArgumentException("LLM API base-url不能为空");
        }

        String normalizedUrl = baseUrl.trim();
        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }

        if (normalizedUrl.endsWith("/chat/completions")) {
            return normalizedUrl;
        }

        if ("https://dashscope.aliyuncs.com/api/v1".equals(normalizedUrl)) {
            normalizedUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }

        return normalizedUrl + "/chat/completions";
    }

    private JSONObject buildFallbackAdjustment(Map<String, Object> planContext) {
        JSONObject result = new JSONObject();
        result.put("needAdjustment", Boolean.FALSE);
        result.put("confidence", 68);
        result.put("summary", "当前信息不足以自动调整，建议先完成最近的未完成任务并观察下一次打卡表现。");
        result.put("actions", new JSONArray());
        JSONArray notes = new JSONArray();
        notes.add("如果连续两次实际用时超过预计 30%，再考虑顺延或拆分后续任务。");
        result.put("notes", notes);
        return result;
    }

    private String trimForPrompt(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String buildPlanPrompt(PlanGenerateRequest request) {
        StringBuilder prompt = new StringBuilder(buildPlanPrompt(
                request.getGoal(),
                request.getLevel(),
                request.getDailyHours().doubleValue(),
                request.getTotalDays()));

        prompt.append("\n\n个性化上下文：\n");
        appendLine(prompt, "学习模式", request.getStudyMode());
        appendLine(prompt, "学习偏好", request.getStudyPreference());
        appendLine(prompt, "预计完成时间", request.getExpectedEndDate());
        appendLine(prompt, "补充说明", request.getNotes());
        appendLine(prompt, "路线预览", request.getRoadmap());
        appendLine(prompt, "目标分析", request.getGoalAnalysis());

        prompt.append("""

                额外要求：
                1. 任务强度要匹配学习模式和每日学习时间。
                2. 优先按路线预览安排前几周内容。
                3. 保持 JSON 结构不变，便于后端保存每日任务。
                """);

        return prompt.toString();
    }

    private String buildPlanPrompt(String goal, String level, double dailyHours, int totalDays) {
        return String.format("""
                你是学习规划师，请生成紧凑、可执行的学习计划。

                学习目标：%s
                当前基础：%s
                每日学习时间：%.1f小时
                计划周期：%d天

                只返回合法 JSON，不要 Markdown，不要解释。字段必须完全匹配：
                {
                  "title": "不超过30字",
                  "summary": "不超过60字",
                  "dailyPlans": [
                    {
                      "day": 1,
                      "content": "简短的今日任务",
                      "duration": %.1f
                    }
                  ]
                }

                要求：
                1. dailyPlans 必须正好 %d 项，day 从 1 递增。
                2. 内容短句化，避免长解释。
                """, goal, level, dailyHours, totalDays, dailyHours, totalDays);
    }

    private JSONObject normalizeGoalAnalysis(JSONObject analysis, String goal, String level) {
        JSONObject fallback = buildFallbackGoalAnalysis(goal, level);

        JSONArray keySkills = analysis.getJSONArray("keySkills");
        if (keySkills == null || keySkills.isEmpty()) {
            keySkills = fallback.getJSONArray("keySkills");
        }

        String recommendedMode = analysis.getString("recommendedMode");
        if (!hasText(recommendedMode)) {
            recommendedMode = fallback.getString("recommendedMode");
        }

        String clarity = normalizeGoalClarity(analysis.getString("clarity"));
        if (!hasText(clarity)) {
            clarity = inferGoalClarity(goal);
        }

        // 内部计算 completionRate 和 tip（不再暴露 recommendedDays/recommendedHours）
        String inferredDifficulty = inferDifficulty(goal);
        String difficulty = normalizeGoalDifficulty(analysis.getString("difficulty"), inferredDifficulty);
        int days = calculateRecommendedDays(goal, level, inferredDifficulty);
        double hours = calculateRecommendedHours(goal, inferredDifficulty);
        int completionRate = calculateCompletionRate(days, hours, level);
        String tip = analysis.getString("tip");
        if (!hasText(tip)) {
            tip = buildAnalysisTip(days, hours);
        }

        JSONObject normalized = new JSONObject();
        normalized.put("keySkills", keySkills);
        normalized.put("clarity", clarity);
        normalized.put("difficulty", difficulty);
        normalized.put("recommendedMode", recommendedMode);
        normalized.put("completionRate", completionRate);
        normalized.put("tip", tip);
        return normalized;
    }

    private JSONObject buildFallbackGoalAnalysis(String goal, String level) {
        String lowerGoal = goal == null ? "" : goal.toLowerCase();

        JSONArray keySkills = new JSONArray();
        if (containsAny(lowerGoal, "java", "spring", "boot", "后端", "接口")) {
            keySkills.add("Java 核心语法"); keySkills.add("Spring Boot"); keySkills.add("MySQL");
        } else if (containsAny(lowerGoal, "vue", "react", "前端", "javascript", "js", "html", "css")) {
            keySkills.add("Vue.js/React"); keySkills.add("JavaScript"); keySkills.add("RESTful API");
        } else if (containsAny(lowerGoal, "python", "爬虫", "自动化")) {
            keySkills.add("Python 基础"); keySkills.add("常用第三方库"); keySkills.add("脚本调试");
        } else if (containsAny(lowerGoal, "数据分析", "pandas", "numpy", "matplotlib", "可视化", "数据")) {
            keySkills.add("Pandas"); keySkills.add("NumPy"); keySkills.add("Matplotlib");
        } else if (containsAny(lowerGoal, "机器学习", "深度学习", "pytorch", "tensorflow", "cnn", "yolo", "opencv")) {
            keySkills.add("PyTorch/TensorFlow"); keySkills.add("模型训练"); keySkills.add("评估指标");
        } else if (containsAny(lowerGoal, "考研", "四级", "六级", "cet", "考试", "期末", "软考", "教资")) {
            keySkills.add("考点梳理"); keySkills.add("题型训练"); keySkills.add("错题复盘");
        } else if (containsAny(lowerGoal, "英语", "雅思", "托福", "口语", "听力", "阅读")) {
            keySkills.add("词汇积累"); keySkills.add("听力阅读"); keySkills.add("口语写作");
        } else if (containsAny(lowerGoal, "算法", "数据结构", "leetcode", "力扣")) {
            keySkills.add("数据结构"); keySkills.add("算法思维"); keySkills.add("复杂度分析");
        } else if (containsAny(lowerGoal, "数据库", "mysql", "redis", "sql")) {
            keySkills.add("SQL 查询"); keySkills.add("表结构设计"); keySkills.add("索引优化");
        } else if (containsAny(lowerGoal, "项目", "毕业设计", "毕设", "系统", "平台")) {
            keySkills.add("需求分析"); keySkills.add("模块设计"); keySkills.add("部署交付");
        } else {
            keySkills.add("核心概念"); keySkills.add("实践练习"); keySkills.add("成果输出");
        }

        String difficulty = inferDifficulty(goal);
        int recommendedDays = calculateRecommendedDays(goal, level, difficulty);
        double recommendedHours = calculateRecommendedHours(goal, difficulty);

        String recommendedMode;
        if (recommendedHours <= 1.2) {
            recommendedMode = "relaxed";
        } else if (recommendedHours <= 2.5) {
            recommendedMode = "standard";
        } else {
            recommendedMode = "sprint";
        }

        JSONObject analysis = new JSONObject();
        analysis.put("keySkills", keySkills);
        analysis.put("clarity", inferGoalClarity(goal));
        analysis.put("difficulty", normalizeGoalDifficulty(null, difficulty));
        analysis.put("recommendedMode", recommendedMode);
        analysis.put("completionRate", calculateCompletionRate(recommendedDays, recommendedHours, level));
        analysis.put("tip", buildAnalysisTip(recommendedDays, recommendedHours));
        return analysis;
    }

    private JSONObject buildFallbackOptimizedGoal(String goal, String level, Integer totalDays, Double dailyHours) {
        String trimmedGoal = cleanGoalText(goal);
        String outcome = buildGoalOutcome(trimmedGoal, level, totalDays, dailyHours);
        String optimized = joinGoalAndOutcome(trimmedGoal, outcome);

        JSONObject result = new JSONObject();
        result.put("goal", limitText(optimized, 120));
        return result;
    }

    private String cleanGoalText(String goal) {
        if (goal == null) {
            return "";
        }
        return goal.trim().replaceAll("[，,。.!！?？;；:：\\s]+$", "");
    }

    private String buildGoalOutcome(String goal, String level, Integer totalDays, Double dailyHours) {
        String lowerGoal = goal == null ? "" : goal.toLowerCase();
        String rhythm = buildGoalRhythm(totalDays, dailyHours);

        if (containsAny(lowerGoal, "java", "spring", "boot", "后端", "接口")) {
            return "掌握 Java 语法、集合、异常处理和面向对象思想，并基于 Spring Boot、MySQL 实现用户登录、数据增删改查和 RESTful 接口模块" + rhythm;
        }
        if (containsAny(lowerGoal, "vue", "react", "前端", "javascript", "js", "html", "css")) {
            return "掌握组件化开发、路由管理、状态管理和接口请求，并完成一个包含登录、列表、表单和数据展示的前端 Demo" + rhythm;
        }
        if (containsAny(lowerGoal, "python", "爬虫", "自动化")) {
            return "掌握 Python 基础语法、文件处理、常用第三方库和脚本调试，并完成一个数据处理或自动化工具 Demo" + rhythm;
        }
        if (containsAny(lowerGoal, "数据分析", "pandas", "numpy", "matplotlib", "可视化", "数据")) {
            return "使用 Pandas、NumPy 和 Matplotlib 完成数据清洗、统计分析、可视化图表和一份可复现的数据分析报告" + rhythm;
        }
        if (containsAny(lowerGoal, "机器学习", "深度学习", "pytorch", "tensorflow", "cnn", "yolo", "opencv", "图像", "视觉")) {
            return "完成数据集整理、模型训练、指标评估和误差分析，并基于 PyTorch/TensorFlow 输出一个可演示的模型实验结果" + rhythm;
        }
        if (containsAny(lowerGoal, "考研", "四级", "六级", "cet", "考试", "期末", "软考", "教资")) {
            return "梳理高频考点，完成专项题型训练、真题限时模拟和错题复盘，形成薄弱模块清单与提分计划" + rhythm;
        }
        if (containsAny(lowerGoal, "英语", "雅思", "托福", "口语", "听力", "阅读", "写作")) {
            return "围绕词汇、听力、阅读、口语和写作建立训练安排，完成每日输入输出练习和阶段性能力自测" + rhythm;
        }
        if (containsAny(lowerGoal, "算法", "数据结构", "leetcode", "力扣")) {
            return "掌握数组、链表、栈队列、树、动态规划和常见复杂度分析，完成分类刷题、错题复盘和模板沉淀" + rhythm;
        }
        if (containsAny(lowerGoal, "数据库", "mysql", "redis", "sql")) {
            return "掌握表结构设计、SQL 查询、索引优化和事务机制，并完成一组业务表设计、查询练习和性能分析记录" + rhythm;
        }
        if (containsAny(lowerGoal, "项目", "毕业设计", "毕设", "系统", "平台")) {
            return "拆解需求、设计模块和数据库表，完成核心功能开发、联调测试、部署演示和项目说明文档" + rhythm;
        }

        return buildGenericOutcome(level, rhythm);
    }

    private String buildGenericOutcome(String level, String rhythm) {
        if ("零基础".equals(level)) {
            return "先建立入门知识框架，再完成基础练习、典型案例拆解和一份阶段学习总结" + rhythm;
        }
        if ("高级".equals(level)) {
            return "聚焦进阶专题、真实案例和综合产出，完成难点突破、实践验证和可复盘的成果材料" + rhythm;
        }
        return "围绕核心概念、关键方法和实践任务推进，完成阶段练习、成果输出和复盘笔记" + rhythm;
    }

    private String buildGoalRhythm(Integer totalDays, Double dailyHours) {
        StringBuilder rhythm = new StringBuilder();
        if (totalDays != null && totalDays > 0) {
            rhythm.append("，计划在").append(totalDays).append("天内完成");
        }
        if (dailyHours != null && dailyHours > 0) {
            rhythm.append("，每天安排").append(formatHours(dailyHours)).append("小时学习");
        }
        return rhythm.toString();
    }

    private String formatHours(Double hours) {
        if (hours == null) {
            return "";
        }
        return hours % 1 == 0 ? String.valueOf(hours.intValue()) : String.valueOf(hours);
    }

    private String joinGoalAndOutcome(String goal, String outcome) {
        if (!hasText(goal)) {
            return outcome;
        }
        if (!hasText(outcome)) {
            return goal;
        }
        return goal + "，" + outcome;
    }

    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String inferDifficulty(String goal) {
        String lowerGoal = goal == null ? "" : goal.toLowerCase();
        if (lowerGoal.contains("源码") || lowerGoal.contains("架构") || lowerGoal.contains("算法") || lowerGoal.contains("机器学习")) {
            return "较高";
        }
        if (lowerGoal.contains("入门") || lowerGoal.contains("基础") || lowerGoal.contains("了解")) {
            return "较低";
        }
        return "中等";
    }

    private String inferGoalClarity(String goal) {
        String text = goal == null ? "" : goal.trim();
        if (text.length() < 8) {
            return "vague";
        }
        if (text.length() < 18) {
            return "broad";
        }
        String lowerGoal = text.toLowerCase();
        if (containsAny(lowerGoal, "项目", "系统", "应用", "demo", "报告", "考试", "证书", "面试",
                "算法", "接口", "模型", "数据", "可视化", "部署", "实战")) {
            return "clear";
        }
        return "focused";
    }

    private String normalizeGoalClarity(String clarity) {
        if (!hasText(clarity)) {
            return null;
        }
        String value = clarity.trim().toLowerCase();
        if (value.contains("clear") || value.contains("清晰") || value.contains("明确")) {
            return "clear";
        }
        if (value.contains("focused") || value.contains("聚焦") || value.contains("较清")) {
            return "focused";
        }
        if (value.contains("vague") || value.contains("模糊") || value.contains("补充")) {
            return "vague";
        }
        if (value.contains("broad") || value.contains("宽泛")) {
            return "broad";
        }
        return null;
    }

    private String normalizeGoalDifficulty(String difficulty, String inferredDifficulty) {
        String value = hasText(difficulty) ? difficulty.trim().toLowerCase() : "";
        if (value.contains("hard") || value.contains("高") || value.contains("难")) {
            return "hard";
        }
        if (value.contains("easy") || value.contains("低") || value.contains("入门") || value.contains("基础")) {
            return "easy";
        }
        if (value.contains("medium") || value.contains("中")) {
            return "medium";
        }
        if ("较高".equals(inferredDifficulty)) {
            return "hard";
        }
        if ("较低".equals(inferredDifficulty)) {
            return "easy";
        }
        return "medium";
    }

    private int calculateRecommendedDays(String goal, String level, String difficulty) {
        Integer explicitDays = extractExplicitDurationDays(goal);
        if (explicitDays != null) {
            return explicitDays;
        }

        String lowerGoal = goal == null ? "" : goal.toLowerCase();
        int days;

        if ("较高".equals(difficulty)) {
            days = 90;
        } else if ("较低".equals(difficulty)) {
            days = 30;
        } else {
            days = 45;
        }

        if (lowerGoal.contains("spring") || lowerGoal.contains("boot") || lowerGoal.contains("后端")) {
            days += 15;
        }
        if (lowerGoal.contains("项目") || lowerGoal.contains("实战") || lowerGoal.contains("毕业设计")) {
            days += 10;
        }
        if (lowerGoal.contains("源码") || lowerGoal.contains("架构") || lowerGoal.contains("算法")) {
            days += 20;
        }
        if (lowerGoal.contains("入门") || lowerGoal.contains("基础")) {
            days -= 10;
        }

        if ("零基础".equals(level)) {
            days += 15;
        } else if ("中级".equals(level)) {
            days -= 8;
        } else if ("高级".equals(level)) {
            days -= 15;
        }

        return Math.max(7, Math.min(30, days));
    }

    private Integer extractExplicitDurationDays(String goal) {
        if (!hasText(goal)) {
            return null;
        }

        Integer numericDays = extractNumericDurationDays(goal);
        if (numericDays != null) {
            return numericDays;
        }

        if (goal.contains("半个月")) {
            return 15;
        }

        Matcher matcher = Pattern.compile("([一二两三四五六七八九十]{1,3})\\s*(天|日|周|星期|礼拜|个月|月)").matcher(goal);
        while (matcher.find()) {
            Integer value = parseChineseNumber(matcher.group(1));
            if (value == null) {
                continue;
            }
            Integer days = convertDurationToDays(value, matcher.group(2));
            if (days != null) {
                return days;
            }
        }
        return null;
    }

    private Integer extractNumericDurationDays(String goal) {
        Matcher matcher = Pattern.compile("(?i)(\\d{1,3})\\s*(天|日|周|星期|礼拜|个月|月|day|days|week|weeks|month|months)").matcher(goal);
        while (matcher.find()) {
            int value;
            try {
                value = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            Integer days = convertDurationToDays(value, matcher.group(2).toLowerCase());
            if (days != null) {
                return days;
            }
        }
        return null;
    }

    private Integer convertDurationToDays(int value, String unit) {
        int days;
        if (unit.contains("周") || unit.contains("星期") || unit.contains("礼拜") || unit.startsWith("week")) {
            days = value * 7;
        } else if (unit.contains("月") || unit.startsWith("month")) {
            days = value * 30;
        } else {
            days = value;
        }
        if (days < 1) {
            return 1;
        }
        return Math.min(30, days);
    }

    private Integer parseChineseNumber(String text) {
        if (!hasText(text)) {
            return null;
        }
        if ("十".equals(text)) {
            return 10;
        }
        int tenIndex = text.indexOf('十');
        if (tenIndex >= 0) {
            int tens = tenIndex == 0 ? 1 : chineseDigitValue(text.charAt(tenIndex - 1));
            int ones = tenIndex == text.length() - 1 ? 0 : chineseDigitValue(text.charAt(tenIndex + 1));
            return tens * 10 + ones;
        }
        return chineseDigitValue(text.charAt(text.length() - 1));
    }

    private int chineseDigitValue(char ch) {
        return switch (ch) {
            case '一' -> 1;
            case '二', '两' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> 0;
        };
    }

    private double calculateRecommendedHours(String goal, String difficulty) {
        String lowerGoal = goal == null ? "" : goal.toLowerCase();
        if ("较高".equals(difficulty) || lowerGoal.contains("项目") || lowerGoal.contains("实战")) {
            return 3.5;
        }
        if ("较低".equals(difficulty)) {
            return 1.5;
        }
        return 2.5;
    }

    private int calculateCompletionRate(int recommendedDays, double recommendedHours, String level) {
        int rate = 86;
        if ("零基础".equals(level)) {
            rate -= 5;
        } else if ("高级".equals(level)) {
            rate += 4;
        }
        if (recommendedDays > 80) {
            rate -= 4;
        }
        if (recommendedHours >= 3.5) {
            rate += 3;
        }
        return Math.max(65, Math.min(96, rate));
    }

    private String buildAnalysisTip(int recommendedDays, double recommendedHours) {
        if (recommendedDays <= 3) {
            return "建议按" + recommendedDays + "天短期冲刺安排，每天学习" + recommendedHours + "小时，优先刷题和复盘薄弱点。";
        }
        if (recommendedDays <= 7) {
            return "建议安排" + recommendedDays + "天专项复习，每天学习" + recommendedHours + "小时，按考点训练并及时整理错题。";
        }
        return "建议安排" + recommendedDays + "天左右，每天学习" + recommendedHours + "小时，并按周复盘。";
    }

    private JSONArray buildFallbackRoadmap(String goal, Integer totalDays) {
        return buildFallbackRoadmap(goal, totalDays, null, null);
    }

    private JSONArray buildFallbackRoadmap(String goal, Integer totalDays, String studyMode, String studyPreference) {
        String modeSuffix = getModeSuffix(studyMode);
        String[][] topics = getRoadmapTopics(goal, studyPreference);

        int days = totalDays != null && totalDays > 0 ? totalDays : 30;
        int stageCount = calculateRoadmapStageCount(days);
        JSONArray roadmap = new JSONArray();
        for (int i = 0; i < stageCount; i++) {
            String[] topic = topics[Math.min(i, topics.length - 1)];
            int[] range = calculateStageRange(days, stageCount, i);
            JSONObject item = new JSONObject();
            item.put("label", buildDayLabel(range[0], range[1]));
            item.put("dayStart", range[0]);
            item.put("dayEnd", range[1]);
            item.put("title", topic[0]);
            item.put("desc", topic[1] + modeSuffix);
            roadmap.add(item);
        }
        return roadmap;
    }

    /**
     * 按学习偏好返回阶段模板，让兜底路线在不同偏好下有明显差异。
     * Spring 相关目标在「偏实战/偏项目」下使用更贴合的技术阶段。
     */
    private String[][] getRoadmapTopics(String goal, String studyPreference) {
        String lowerGoal = goal == null ? "" : goal.toLowerCase();
        boolean isSpring = lowerGoal.contains("spring") || lowerGoal.contains("boot");

        if ("theory".equals(studyPreference)) {
            return new String[][]{
                    {"概念框架", "梳理核心概念与术语体系"},
                    {"原理精读", "深入理解底层原理与机制"},
                    {"知识体系", "搭建完整知识地图与脉络"},
                    {"案例剖析", "结合典型案例理解原理应用"},
                    {"体系梳理", "对比归纳，打通知识点关联"},
                    {"笔记总结", "整理思维导图与原理总结"}
            };
        }
        if ("exam".equals(studyPreference)) {
            return new String[][]{
                    {"考点梳理", "通览大纲，标记高频考点"},
                    {"基础刷题", "按章节完成基础题型训练"},
                    {"专项突破", "针对难点题型集中训练"},
                    {"真题演练", "限时模拟真题与考试节奏"},
                    {"错题复盘", "整理错题本，回炉薄弱点"},
                    {"冲刺模考", "全真模考查漏补缺"}
            };
        }
        if ("project".equals(studyPreference)) {
            if (isSpring) {
                return new String[][]{
                        {"需求与选型", "拆解需求，确定技术栈"},
                        {"环境与骨架", "搭建 Spring Boot 项目骨架"},
                        {"核心模块", "实现接口、服务与数据层"},
                        {"集成联调", "对接数据库与前后端联调"},
                        {"部署上线", "打包部署与日志监控"},
                        {"作品打磨", "完善文档与项目展示"}
                };
            }
            return new String[][]{
                    {"需求拆解", "明确目标，拆分项目里程碑"},
                    {"技术选型", "确定技术栈与项目结构"},
                    {"模块开发", "逐个实现核心功能模块"},
                    {"集成联调", "整合模块并调试完整流程"},
                    {"部署上线", "完成部署、测试与发布"},
                    {"作品打磨", "优化体验并整理作品材料"}
            };
        }

        // 默认：偏实战 practice
        if (isSpring) {
            return new String[][]{
                    {"Java基础", "动手练语法、集合、异常"},
                    {"Spring核心", "实操 IOC、AOP 与 Bean"},
                    {"Spring Boot", "手写接口与参数校验"},
                    {"MySQL与接口", "表设计、分页与接口联调"},
                    {"项目实战", "完成一个完整业务模块"},
                    {"部署与优化", "上线部署、日志与调优"}
            };
        }
        return new String[][]{
                {"快速上手", "跑通环境，动手第一个示例"},
                {"核心语法", "边学边练核心语法点"},
                {"动手练习", "完成针对性编码练习"},
                {"小项目实操", "用所学做一个小项目"},
                {"综合练习", "串联知识点做综合训练"},
                {"即用复盘", "复盘问题并沉淀可复用经验"}
        };
    }

    private String describeStudyMode(String studyMode) {
        if ("relaxed".equals(studyMode)) {
            return "轻松模式：降低每日任务密度，更多复习和缓冲。";
        }
        if ("sprint".equals(studyMode)) {
            return "冲刺模式：提高任务密度，阶段更紧凑，增加实操产出。";
        }
        return "标准模式：概念、练习、复盘节奏均衡。";
    }

    private String describeStudyPreference(String studyPreference) {
        if ("theory".equals(studyPreference)) {
            return "偏理论：阶段描述侧重概念框架、原理、知识体系。";
        }
        if ("exam".equals(studyPreference)) {
            return "偏考试：阶段描述侧重题型训练、错题复盘、考点覆盖。";
        }
        if ("project".equals(studyPreference)) {
            return "偏项目：阶段描述侧重作品产出、模块交付、部署展示。";
        }
        return "偏实战：阶段描述侧重练习、动手实现、即学即用。";
    }

    private String getModeSuffix(String studyMode) {
        if ("relaxed".equals(studyMode)) {
            return "，保留复盘缓冲";
        }
        if ("sprint".equals(studyMode)) {
            return "，提高交付强度";
        }
        return "，节奏保持均衡";
    }

    private JSONArray normalizeRoadmap(JSONArray source, String goal, int totalDays) {
        int stageCount = calculateRoadmapStageCount(totalDays);
        if (source == null || source.isEmpty()) {
            return buildFallbackRoadmap(goal, totalDays);
        }

        JSONArray normalized = new JSONArray();
        for (int i = 0; i < stageCount; i++) {
            JSONObject raw = i < source.size() && source.get(i) instanceof JSONObject
                    ? source.getJSONObject(i)
                    : new JSONObject();
            int[] range = calculateStageRange(totalDays, stageCount, i);
            JSONObject item = new JSONObject();
            item.put("label", buildDayLabel(range[0], range[1]));
            item.put("dayStart", range[0]);
            item.put("dayEnd", range[1]);
            item.put("title", hasText(raw.getString("title")) ? raw.getString("title") : "阶段学习");
            item.put("desc", hasText(raw.getString("desc")) ? raw.getString("desc") : "完成本阶段核心内容");
            normalized.add(item);
        }
        return normalized;
    }

    private int calculateRoadmapStageCount(int totalDays) {
        if (totalDays <= 14) {
            return totalDays;
        }
        if (totalDays <= 30) {
            return Math.min(10, (int) Math.ceil(totalDays / 3.0));
        }
        if (totalDays <= 60) {
            return Math.min(10, (int) Math.ceil(totalDays / 7.0));
        }
        return Math.min(12, (int) Math.ceil(totalDays / 14.0));
    }

    private int[] calculateStageRange(int totalDays, int stageCount, int index) {
        int start = (int) Math.floor(index * (double) totalDays / stageCount) + 1;
        int end = (int) Math.floor((index + 1) * (double) totalDays / stageCount);
        return new int[]{start, Math.max(start, end)};
    }

    private String buildDayLabel(int start, int end) {
        return start == end ? "第" + start + "天" : "第" + start + "-" + end + "天";
    }

    private JSONObject parseObject(String llmResponse) {
        String json = extractJson(llmResponse, "{", "}");
        return JSON.parseObject(json);
    }

    private JSONArray parseArray(String llmResponse) {
        String json = extractJson(llmResponse, "[", "]");
        return JSON.parseArray(json);
    }

    private String extractJson(String text, String startToken, String endToken) {
        int start = text.indexOf(startToken);
        int end = text.lastIndexOf(endToken);
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void appendLine(StringBuilder builder, String label, Object value) {
        if (value == null || !hasText(value.toString())) {
            return;
        }
        builder.append("- ").append(label).append(": ").append(value).append("\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
