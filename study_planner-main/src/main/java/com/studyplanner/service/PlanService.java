package com.studyplanner.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.studyplanner.dto.PlanGenerateRequest;
import com.studyplanner.dto.PlanTaskEditRequest;
import com.studyplanner.entity.CheckIn;
import com.studyplanner.entity.PlanEditVersion;
import com.studyplanner.entity.PlanDetail;
import com.studyplanner.entity.StudyPlan;
import com.studyplanner.mapper.CheckInMapper;
import com.studyplanner.mapper.PlanEditVersionMapper;
import com.studyplanner.mapper.PlanDetailMapper;
import com.studyplanner.mapper.PlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanService {

    private static final String STATUS_IN_PROGRESS = "\u8fdb\u884c\u4e2d";
    private static final String STATUS_COMPLETED = "\u5df2\u5b8c\u6210";
    private static final String TYPE_PERSONAL = "personal";

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private PlanDetailMapper planDetailMapper;

    @Autowired
    private PlanEditVersionMapper planEditVersionMapper;

    @Autowired
    private LLMService llmService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlanGroupService planGroupService;

    @Autowired
    private CheckInMapper checkInMapper;

    @Transactional
    public StudyPlan generatePlan(Long userId, PlanGenerateRequest request) {
        ensurePlanWorkbenchSchema();
        String llmResponse = llmService.generateStudyPlan(request);

        JSONObject planJson = parseLLMResponseWithRepair(llmResponse, request);
        StudyPlan plan = buildPlan(userId, request, planJson);

        planMapper.insert(plan);
        planGroupService.ensureSchema();
        planGroupService.ensureOwnerMember(plan.getId(), userId);
        savePlanDetails(plan, planJson, request);

        return plan;
    }

    public JSONObject generatePlanForGuest(PlanGenerateRequest request) {
        String llmResponse = llmService.generateStudyPlanWithCustomConfig(request);

        JSONObject planJson = parseLLMResponseWithRepair(llmResponse, request);
        planJson.put("isGuestPlan", true);
        planJson.put("message", "Guest preview only. Log in to save this plan.");
        return planJson;
    }

    private StudyPlan buildPlan(Long userId, PlanGenerateRequest request, JSONObject planJson) {
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = planJson.getString("title");
        }
        if (title == null || title.trim().isEmpty()) {
            title = request.getGoal() + " Plan";
        }

        plan.setTitle(title);
        plan.setGoal(request.getGoal());
        plan.setLevel(request.getLevel());
        plan.setDailyHours(request.getDailyHours());
        plan.setTotalDays(request.getTotalDays());
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(request.getTotalDays() - 1));
        plan.setStatus(STATUS_IN_PROGRESS);
        plan.setPlanType(TYPE_PERSONAL);
        return plan;
    }

    private JSONObject parseLLMResponse(String llmResponse, PlanGenerateRequest request) {
        try {
            String jsonStr = llmResponse;
            int jsonStart = llmResponse.indexOf("{");
            int jsonEnd = llmResponse.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonStr = llmResponse.substring(jsonStart, jsonEnd + 1);
            }
            return JSON.parseObject(jsonStr);
        } catch (RuntimeException e) {
            return buildFallbackPlanJson(request);
        }
    }

    private JSONObject parseLLMResponseWithRepair(String llmResponse, PlanGenerateRequest request) {
        JSONObject parsed;
        try {
            parsed = parseRawPlanJson(llmResponse);
        } catch (RuntimeException e) {
            try {
                String repairedResponse = llmService.repairStudyPlanResponse(request, llmResponse, "原始输出不是合法 JSON: " + e.getMessage());
                parsed = parseRawPlanJson(repairedResponse);
            } catch (RuntimeException repairError) {
                JSONObject fallback = buildFallbackPlanJson(request);
                fallback.put("qualityScore", 50);
                fallback.put("qualityWarning", "原始输出和修复输出都无法解析为合法 JSON");
                return fallback;
            }
        }
        JSONObject normalized = normalizePlanJson(parsed, request);
        int qualityScore = scorePlanJson(normalized, request);
        if (qualityScore >= 85) {
            normalized.put("qualityScore", qualityScore);
            return normalized;
        }

        String qualityReport = buildPlanQualityReport(normalized, request, qualityScore);
        try {
            String repairedResponse = llmService.repairStudyPlanResponse(request, llmResponse, qualityReport);
            JSONObject repaired = normalizePlanJson(parseRawPlanJson(repairedResponse), request);
            int repairedScore = scorePlanJson(repaired, request);
            repaired.put("qualityScore", repairedScore);
            if (repairedScore >= 75) {
                return repaired;
            }
            System.err.println("LLM plan repair quality is still low: " + buildPlanQualityReport(repaired, request, repairedScore));
        } catch (RuntimeException e) {
            System.err.println("Failed to repair LLM plan response: " + e.getMessage());
        }

        JSONObject fallback = buildFallbackPlanJson(request);
        fallback.put("qualityScore", 60);
        fallback.put("qualityWarning", qualityReport);
        return fallback;
    }

    private JSONObject parseRawPlanJson(String llmResponse) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty LLM response");
        }
        String jsonStr = llmResponse;
        int jsonStart = llmResponse.indexOf("{");
        int jsonEnd = llmResponse.lastIndexOf("}");
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            jsonStr = llmResponse.substring(jsonStart, jsonEnd + 1);
        }
        return JSON.parseObject(jsonStr);
    }

    private JSONObject normalizePlanJson(JSONObject source, PlanGenerateRequest request) {
        JSONObject normalized = new JSONObject();
        String title = source != null ? source.getString("title") : null;
        String summary = source != null ? source.getString("summary") : null;
        normalized.put("title", hasText(title) ? limitText(title.trim(), 40) : limitText(request.getGoal() + "计划", 40));
        normalized.put("summary", hasText(summary) ? limitText(summary.trim(), 120)
                : request.getTotalDays() + "天个性化学习计划，每天约" + request.getDailyHours() + "小时。");

        JSONArray rawDailyPlans = source != null ? source.getJSONArray("dailyPlans") : null;
        JSONArray dailyPlans = new JSONArray();
        for (int day = 1; day <= request.getTotalDays(); day++) {
            JSONObject raw = rawDailyPlans != null && day <= rawDailyPlans.size() && rawDailyPlans.get(day - 1) instanceof JSONObject
                    ? rawDailyPlans.getJSONObject(day - 1)
                    : null;
            JSONObject item = raw != null ? raw : buildFallbackDay(request, day);
            JSONObject normalizedItem = new JSONObject();
            normalizedItem.put("day", day);
            normalizedItem.put("content", normalizeTaskContent(item.getString("content"), request, day));
            normalizedItem.put("duration", clampDuration(readDouble(item, "duration", request.getDailyHours().doubleValue())));
            dailyPlans.add(normalizedItem);
        }
        normalized.put("dailyPlans", dailyPlans);
        return normalized;
    }

    private int scorePlanJson(JSONObject planJson, PlanGenerateRequest request) {
        int score = 0;
        if (hasText(planJson.getString("title"))) {
            score += 15;
        }
        if (hasText(planJson.getString("summary"))) {
            score += 15;
        }
        JSONArray dailyPlans = planJson.getJSONArray("dailyPlans");
        if (dailyPlans != null && dailyPlans.size() == request.getTotalDays()) {
            score += 25;
        }
        int goodDays = 0;
        if (dailyPlans != null) {
            for (int i = 0; i < dailyPlans.size(); i++) {
                JSONObject item = dailyPlans.getJSONObject(i);
                String content = item.getString("content");
                Double duration = item.getDouble("duration");
                if (item.getIntValue("day") == i + 1
                        && hasText(content)
                        && content.length() >= 8
                        && !content.contains("学习并完成练习")
                        && duration != null
                        && duration >= 0.5
                        && duration <= 12) {
                    goodDays++;
                }
            }
        }
        if (request.getTotalDays() > 0) {
            score += Math.round(45f * goodDays / request.getTotalDays());
        }
        return Math.min(100, score);
    }

    private String buildPlanQualityReport(JSONObject planJson, PlanGenerateRequest request, int score) {
        JSONArray dailyPlans = planJson.getJSONArray("dailyPlans");
        int count = dailyPlans != null ? dailyPlans.size() : 0;
        return "qualityScore=" + score
                + "; expectedDays=" + request.getTotalDays()
                + "; actualDays=" + count
                + "; titleOk=" + hasText(planJson.getString("title"))
                + "; summaryOk=" + hasText(planJson.getString("summary"));
    }

    private void savePlanDetails(StudyPlan plan, JSONObject planJson, PlanGenerateRequest request) {
        JSONArray dailyPlans = planJson.getJSONArray("dailyPlans");
        if (dailyPlans == null) {
            dailyPlans = new JSONArray();
        }

        for (int day = 1; day <= request.getTotalDays(); day++) {
            JSONObject dayPlan = day <= dailyPlans.size() ? dailyPlans.getJSONObject(day - 1) : null;
            if (dayPlan == null) {
                dayPlan = buildFallbackDay(request, day);
            }

            PlanDetail detail = new PlanDetail();
            detail.setPlanId(plan.getId());
            detail.setDayNumber(readInt(dayPlan, "day", day));
            detail.setContent(readString(dayPlan, "content",
                    "Day " + day + ": study " + request.getGoal() + " and complete practice"));
            detail.setDuration(BigDecimal.valueOf(readDouble(dayPlan, "duration", request.getDailyHours().doubleValue())));
            detail.setScheduledDate(plan.getStartDate().plusDays(day - 1L));
            detail.setSortOrder(day);
            detail.setIsCompleted(0);

            planDetailMapper.insert(detail);
        }
    }

    private JSONObject buildFallbackPlanJson(PlanGenerateRequest request) {
        JSONObject planJson = new JSONObject();
        planJson.put("title", request.getGoal() + " Plan");
        planJson.put("summary", request.getTotalDays() + " day study plan");

        JSONArray dailyPlans = new JSONArray();
        for (int day = 1; day <= request.getTotalDays(); day++) {
            dailyPlans.add(buildFallbackDay(request, day));
        }
        planJson.put("dailyPlans", dailyPlans);
        return planJson;
    }

    private JSONObject buildFallbackDay(PlanGenerateRequest request, int day) {
        JSONObject dayPlan = new JSONObject();
        dayPlan.put("day", day);
        dayPlan.put("content", "Day " + day + ": study " + request.getGoal() + " and complete practice");
        dayPlan.put("duration", request.getDailyHours());
        return dayPlan;
    }

    public Map<String, Object> askDraftAdvisor(Long userId, Map<String, Object> request) {
        String question = readMapString(request, "question");
        if (!hasText(question)) {
            question = "请根据当前草稿判断学习目标、周期和每日学习时长是否合理。";
        }

        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("draft", request != null ? request.get("draft") : null);
        context.put("goalAnalysis", request != null ? request.get("goalAnalysis") : null);
        context.put("roadmap", request != null ? request.get("roadmap") : null);

        String reply = llmService.generateDraftAssistantReply(context, question);
        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("scene", "draft");
        return result;
    }

    public Map<String, Object> askPlanAdvisor(Long planId, Long userId, Map<String, Object> request) {
        StudyPlan plan = validatePlanMember(planId, userId);
        String question = readMapString(request, "question");
        if (!hasText(question)) {
            question = "请根据当前计划进度给我一个下一步学习建议。";
        }

        Map<String, Object> context = buildPlanAssistantContext(plan, userId, request);
        String reply = llmService.generatePlanAssistantReply(context, question);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("metrics", context.get("metrics"));
        result.put("todayTask", context.get("todayTask"));
        return result;
    }

    public JSONObject previewAdaptiveAdjustment(Long planId, Long userId, Map<String, Object> request) {
        StudyPlan plan = validatePlanManager(planId, userId);
        Map<String, Object> context = buildPlanAssistantContext(plan, userId, request);
        JSONObject suggestion = llmService.suggestPlanAdjustment(context);
        return normalizeAdjustmentSuggestion(plan, suggestion);
    }

    @Transactional
    public List<PlanDetail> applyAdaptiveAdjustment(Long planId, Long userId, Map<String, Object> request) {
        StudyPlan plan = validatePlanManager(planId, userId);
        JSONObject suggestion;
        Object providedSuggestion = request != null ? request.get("suggestion") : null;
        if (providedSuggestion != null) {
            suggestion = normalizeAdjustmentSuggestion(plan, JSON.parseObject(JSON.toJSONString(providedSuggestion)));
        } else {
            suggestion = previewAdaptiveAdjustment(planId, userId, request);
        }
        JSONArray actions = suggestion.getJSONArray("actions");
        if (actions == null || actions.isEmpty()) {
            return getPlanDetails(planId);
        }

        createVersionSnapshot(plan, userId, "AI自适应调整");
        for (int i = 0; i < actions.size(); i++) {
            JSONObject action = actions.getJSONObject(i);
            Long detailId = action.getLong("detailId");
            if (detailId == null) {
                continue;
            }
            PlanDetail detail = planDetailMapper.findById(detailId);
            if (detail == null || !planId.equals(detail.getPlanId()) || Integer.valueOf(1).equals(detail.getIsCompleted())) {
                continue;
            }
            String content = action.getString("content");
            String scheduledDate = action.getString("scheduledDate");
            Double duration = action.getDouble("duration");

            if (hasText(content)) {
                detail.setContent(limitText(content.trim(), 180));
            }
            if (hasText(scheduledDate)) {
                detail.setScheduledDate(LocalDate.parse(scheduledDate));
                detail.setDayNumber(calculateDayNumber(plan.getStartDate(), detail.getScheduledDate()));
            }
            if (duration != null) {
                detail.setDuration(BigDecimal.valueOf(clampDuration(duration)));
            }
            planDetailMapper.updateTask(detail);
        }
        return refreshPlanRange(plan);
    }

    private Map<String, Object> buildPlanAssistantContext(StudyPlan plan, Long userId, Map<String, Object> request) {
        List<PlanDetail> details = planDetailMapper.findByPlanId(plan.getId());
        normalizeSchedule(plan, details, false);
        List<CheckIn> checkIns = checkInMapper.findByPlanIdAndUserId(plan.getId(), userId);
        Map<Long, CheckIn> checkInByDetailId = new HashMap<>();
        for (CheckIn checkIn : checkIns) {
            checkInByDetailId.put(checkIn.getDetailId(), checkIn);
        }

        LocalDate today = LocalDate.now();
        int completed = 0;
        double plannedHours = 0;
        double actualHours = 0;
        int overdue = 0;
        JSONArray recentTasks = new JSONArray();
        JSONArray futureTasks = new JSONArray();
        JSONObject todayTask = null;

        for (PlanDetail detail : details) {
            boolean done = Integer.valueOf(1).equals(detail.getIsCompleted()) || checkInByDetailId.containsKey(detail.getId());
            if (done) {
                completed++;
            }
            plannedHours += detail.getDuration() != null ? detail.getDuration().doubleValue() : 0;
            CheckIn checkIn = checkInByDetailId.get(detail.getId());
            if (checkIn != null && checkIn.getStudyHours() != null) {
                actualHours += checkIn.getStudyHours().doubleValue();
            }
            if (!done && detail.getScheduledDate() != null && detail.getScheduledDate().isBefore(today)) {
                overdue++;
            }

            JSONObject task = buildTaskContext(detail, done, checkIn);
            if (detail.getScheduledDate() != null && detail.getScheduledDate().equals(today)) {
                todayTask = task;
            }
            if (recentTasks.size() < 8 && (done || (detail.getScheduledDate() != null && !detail.getScheduledDate().isAfter(today)))) {
                recentTasks.add(task);
            }
            if (!done && futureTasks.size() < 12) {
                futureTasks.add(task);
            }
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalTasks", details.size());
        metrics.put("completedTasks", completed);
        metrics.put("progress", details.isEmpty() ? 0 : Math.round(completed * 1000.0 / details.size()) / 10.0);
        metrics.put("overdueTasks", overdue);
        metrics.put("plannedHours", Math.round(plannedHours * 10.0) / 10.0);
        metrics.put("actualHours", Math.round(actualHours * 10.0) / 10.0);
        metrics.put("hourVariance", Math.round((actualHours - plannedHours * completed / Math.max(1, details.size())) * 10.0) / 10.0);

        Map<String, Object> context = new HashMap<>();
        Map<String, Object> planInfo = new HashMap<>();
        planInfo.put("id", plan.getId());
        planInfo.put("title", plan.getTitle());
        planInfo.put("goal", plan.getGoal());
        planInfo.put("level", plan.getLevel());
        planInfo.put("dailyHours", plan.getDailyHours());
        planInfo.put("totalDays", plan.getTotalDays());
        planInfo.put("startDate", plan.getStartDate());
        planInfo.put("endDate", plan.getEndDate());
        planInfo.put("status", plan.getStatus());
        context.put("plan", planInfo);
        context.put("metrics", metrics);
        context.put("todayTask", todayTask);
        context.put("recentTasks", recentTasks);
        context.put("futureTasks", futureTasks);
        context.put("latestCheckIn", request != null ? request.get("latestCheckIn") : null);
        context.put("userQuestion", request != null ? request.get("question") : null);
        context.put("extra", request);
        return context;
    }

    private JSONObject buildTaskContext(PlanDetail detail, boolean done, CheckIn checkIn) {
        JSONObject task = new JSONObject();
        task.put("detailId", detail.getId());
        task.put("dayNumber", detail.getDayNumber());
        task.put("content", detail.getContent());
        task.put("duration", detail.getDuration());
        task.put("scheduledDate", detail.getScheduledDate());
        task.put("completed", done);
        if (checkIn != null) {
            task.put("actualHours", checkIn.getStudyHours());
            task.put("checkDate", checkIn.getCheckDate());
            task.put("note", checkIn.getNote());
        }
        return task;
    }

    private JSONObject normalizeAdjustmentSuggestion(StudyPlan plan, JSONObject source) {
        List<PlanDetail> details = planDetailMapper.findByPlanId(plan.getId());
        Map<Long, PlanDetail> editableById = new HashMap<>();
        for (PlanDetail detail : details) {
            if (!Integer.valueOf(1).equals(detail.getIsCompleted())) {
                editableById.put(detail.getId(), detail);
            }
        }

        JSONObject normalized = new JSONObject();
        boolean needAdjustment = source != null && Boolean.TRUE.equals(source.getBoolean("needAdjustment"));
        normalized.put("needAdjustment", needAdjustment);
        normalized.put("confidence", source != null && source.getInteger("confidence") != null
                ? Math.max(0, Math.min(100, source.getInteger("confidence")))
                : 60);
        normalized.put("summary", source != null && hasText(source.getString("summary"))
                ? limitText(source.getString("summary"), 160)
                : "当前计划暂不需要大幅调整。");

        JSONArray actions = new JSONArray();
        JSONArray rawActions = source != null ? source.getJSONArray("actions") : null;
        if (rawActions != null) {
            for (int i = 0; i < rawActions.size() && actions.size() < 8; i++) {
                JSONObject raw = rawActions.getJSONObject(i);
                Long detailId = raw.getLong("detailId");
                PlanDetail detail = detailId != null ? editableById.get(detailId) : null;
                if (detail == null) {
                    continue;
                }
                JSONObject action = new JSONObject();
                action.put("detailId", detailId);
                action.put("content", hasText(raw.getString("content"))
                        ? limitText(raw.getString("content").trim(), 180)
                        : detail.getContent());
                action.put("duration", clampDuration(raw.getDouble("duration") != null
                        ? raw.getDouble("duration")
                        : (detail.getDuration() != null ? detail.getDuration().doubleValue() : plan.getDailyHours().doubleValue())));
                action.put("scheduledDate", normalizeActionDate(raw.getString("scheduledDate"), detail));
                action.put("reason", hasText(raw.getString("reason")) ? limitText(raw.getString("reason"), 120) : "根据当前进度调整");
                actions.add(action);
            }
        }
        normalized.put("actions", actions);
        normalized.put("needAdjustment", needAdjustment && !actions.isEmpty());

        JSONArray notes = source != null && source.getJSONArray("notes") != null ? source.getJSONArray("notes") : new JSONArray();
        normalized.put("notes", notes);
        return normalized;
    }

    private String normalizeActionDate(String value, PlanDetail detail) {
        if (hasText(value)) {
            try {
                return LocalDate.parse(value.trim().substring(0, 10)).toString();
            } catch (RuntimeException ignored) {
                // fall through to current scheduled date
            }
        }
        LocalDate fallback = detail.getScheduledDate() != null ? detail.getScheduledDate() : LocalDate.now();
        return fallback.toString();
    }

    private int readInt(JSONObject json, String key, int defaultValue) {
        Integer value = json.getInteger(key);
        return value != null ? value : defaultValue;
    }

    private double readDouble(JSONObject json, String key, double defaultValue) {
        BigDecimal value = json.getBigDecimal(key);
        return value != null ? value.doubleValue() : defaultValue;
    }

    private String readString(JSONObject json, String key, String defaultValue) {
        String value = json.getString(key);
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
    }

    private String readMapString(Map<String, Object> request, String key) {
        if (request == null) {
            return null;
        }
        Object value = request.get(key);
        return value != null ? value.toString() : null;
    }

    private String normalizeTaskContent(String content, PlanGenerateRequest request, int day) {
        if (!hasText(content)) {
            return "第" + day + "天：围绕" + request.getGoal() + "完成知识学习、练习和复盘";
        }
        return limitText(content.trim(), 180);
    }

    private double clampDuration(double value) {
        return Math.max(0.5, Math.min(12.0, Math.round(value * 10.0) / 10.0));
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public List<StudyPlan> getUserPlans(Long userId) {
        List<StudyPlan> plans = planMapper.findByUserId(userId);
        for (StudyPlan plan : plans) {
            double progress = getPlanProgress(plan.getId());
            plan.setProgress(progress);

            if (progress >= 100.0 && STATUS_IN_PROGRESS.equals(plan.getStatus())) {
                updatePlanStatus(plan.getId(), STATUS_COMPLETED);
                plan.setStatus(STATUS_COMPLETED);
            }
        }
        return plans;
    }

    public void updatePlan(Long planId, Long userId, String title, String goal) {
        StudyPlan plan = validatePlanManager(planId, userId);
        plan.setTitle(title);
        plan.setGoal(goal);
        planMapper.update(plan);
    }

    public StudyPlan getPlanWithDetails(Long planId) {
        return planMapper.findById(planId);
    }

    public List<PlanDetail> getPlanDetails(Long planId) {
        StudyPlan plan = planMapper.findById(planId);
        List<PlanDetail> details = planDetailMapper.findByPlanId(planId);
        normalizeSchedule(plan, details, false);
        return details;
    }

    public Map<String, Object> getPlanSchedule(Long planId, Long userId) {
        ensurePlanWorkbenchSchema();
        StudyPlan plan = validatePlanManager(planId, userId);
        List<PlanDetail> details = planDetailMapper.findByPlanId(planId);
        normalizeSchedule(plan, details, true);

        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("details", details);
        result.put("conflicts", buildScheduleConflicts(details));
        result.put("versions", planEditVersionMapper.findByPlanId(planId));
        return result;
    }

    @Transactional
    public List<PlanDetail> updateTaskSchedule(Long planId, Long userId, List<PlanTaskEditRequest> items) {
        ensurePlanWorkbenchSchema();
        StudyPlan plan = validatePlanManager(planId, userId);
        createVersionSnapshot(plan, userId, "拖拽调整排期");

        for (PlanTaskEditRequest item : items) {
            if (item.getId() == null || item.getScheduledDate() == null) {
                continue;
            }
            PlanDetail detail = planDetailMapper.findById(item.getId());
            if (detail == null || !planId.equals(detail.getPlanId())) {
                continue;
            }
            detail.setScheduledDate(item.getScheduledDate());
            detail.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : detail.getDayNumber());
            detail.setDayNumber(calculateDayNumber(plan.getStartDate(), item.getScheduledDate()));
            planDetailMapper.updateSchedule(detail);
        }

        return refreshPlanRange(plan);
    }

    @Transactional
    public List<PlanDetail> batchUpdateTasks(Long planId, Long userId, List<PlanTaskEditRequest> items) {
        ensurePlanWorkbenchSchema();
        StudyPlan plan = validatePlanManager(planId, userId);
        createVersionSnapshot(plan, userId, "批量编辑任务");

        int fallbackOrder = 1;
        for (PlanTaskEditRequest item : items) {
            PlanDetail detail = item.getId() != null ? planDetailMapper.findById(item.getId()) : new PlanDetail();
            if (detail == null || (detail.getId() != null && !planId.equals(detail.getPlanId()))) {
                continue;
            }

            detail.setPlanId(planId);
            detail.setDayNumber(item.getDayNumber() != null ? item.getDayNumber() : fallbackOrder);
            detail.setContent(hasText(item.getContent()) ? item.getContent().trim() : "学习任务");
            detail.setDuration(item.getDuration() != null ? item.getDuration() : plan.getDailyHours());
            detail.setScheduledDate(item.getScheduledDate() != null
                    ? item.getScheduledDate()
                    : plan.getStartDate().plusDays(Math.max(0, detail.getDayNumber() - 1L)));
            detail.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : fallbackOrder);
            detail.setIsCompleted(item.getIsCompleted() != null ? item.getIsCompleted() : 0);

            if (detail.getId() == null) {
                planDetailMapper.insert(detail);
            } else {
                planDetailMapper.updateTask(detail);
            }
            fallbackOrder++;
        }

        return refreshPlanRange(plan);
    }

    @Transactional
    public PlanDetail addTask(Long planId, Long userId, PlanTaskEditRequest request) {
        ensurePlanWorkbenchSchema();
        StudyPlan plan = validatePlanManager(planId, userId);
        createVersionSnapshot(plan, userId, "新增任务");

        List<PlanDetail> details = planDetailMapper.findByPlanId(planId);
        int nextDay = details.stream().map(PlanDetail::getDayNumber).filter(v -> v != null).max(Integer::compareTo).orElse(0) + 1;

        PlanDetail detail = new PlanDetail();
        detail.setPlanId(planId);
        detail.setDayNumber(request.getDayNumber() != null ? request.getDayNumber() : nextDay);
        detail.setContent(hasText(request.getContent()) ? request.getContent().trim() : "新学习任务");
        detail.setDuration(request.getDuration() != null ? request.getDuration() : plan.getDailyHours());
        detail.setScheduledDate(request.getScheduledDate() != null
                ? request.getScheduledDate()
                : plan.getStartDate().plusDays(Math.max(0, detail.getDayNumber() - 1L)));
        detail.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextDay);
        detail.setIsCompleted(0);
        planDetailMapper.insert(detail);
        refreshPlanRange(plan);
        return detail;
    }

    @Transactional
    public void deleteTask(Long planId, Long detailId, Long userId) {
        ensurePlanWorkbenchSchema();
        StudyPlan plan = validatePlanOwner(planId, userId);
        createVersionSnapshot(plan, userId, "删除任务");
        planDetailMapper.deleteByIdAndPlanId(detailId, planId);
        refreshPlanRange(plan);
    }

    public List<PlanEditVersion> getVersions(Long planId, Long userId) {
        ensurePlanWorkbenchSchema();
        validatePlanManager(planId, userId);
        return planEditVersionMapper.findByPlanId(planId);
    }

    @Transactional
    public List<PlanDetail> restoreVersion(Long planId, Long versionId, Long userId) {
        ensurePlanWorkbenchSchema();
        StudyPlan plan = validatePlanManager(planId, userId);
        PlanEditVersion version = planEditVersionMapper.findByIdAndPlanId(versionId, planId);
        if (version == null) {
            throw new IllegalArgumentException("Version does not exist");
        }

        createVersionSnapshot(plan, userId, "回滚前自动备份");
        List<PlanDetail> snapshot = JSON.parseArray(version.getSnapshot(), PlanDetail.class);
        planDetailMapper.deleteByPlanId(planId);
        for (PlanDetail detail : snapshot) {
            detail.setId(null);
            detail.setPlanId(planId);
            planDetailMapper.insert(detail);
        }
        return refreshPlanRange(plan);
    }

    public void updatePlanStatus(Long planId, String status) {
        planMapper.updateStatus(planId, status);
    }

    @Transactional
    public void deletePlan(Long planId, Long userId) {
        StudyPlan plan = validatePlanOwner(planId, userId);
        planDetailMapper.deleteByPlanId(planId);
        planMapper.delete(plan.getId());
    }

    public PlanDetail getTodayTask(Long planId, Long userId) {
        StudyPlan plan = validatePlanMember(planId, userId);
        long dayNumber = LocalDate.now().toEpochDay() - plan.getStartDate().toEpochDay() + 1;

        if (dayNumber < 1 || dayNumber > plan.getTotalDays()) {
            return null;
        }

        PlanDetail task = planDetailMapper.findByPlanIdAndDay(planId, (int) dayNumber);
        if (task == null) {
            return null;
        }

        CheckIn todayCheckIn = checkInMapper.findByUserIdAndDateAndDetailId(userId, LocalDate.now(), task.getId());
        if (todayCheckIn != null) {
            task.setIsCompleted(1);
        }

        return task;
    }

    public double getPlanProgress(Long planId) {
        List<PlanDetail> details = planDetailMapper.findByPlanId(planId);
        if (details.isEmpty()) {
            return 0;
        }

        int completed = planDetailMapper.countCompletedByPlanId(planId);
        return (double) completed / details.size() * 100;
    }

    private void normalizeSchedule(StudyPlan plan, List<PlanDetail> details, boolean persist) {
        if (plan == null) {
            return;
        }
        int index = 1;
        for (PlanDetail detail : details) {
            boolean changed = false;
            if (detail.getScheduledDate() == null) {
                int day = detail.getDayNumber() != null ? detail.getDayNumber() : index;
                detail.setScheduledDate(plan.getStartDate().plusDays(Math.max(0, day - 1L)));
                changed = true;
            }
            if (detail.getSortOrder() == null) {
                detail.setSortOrder(index);
                changed = true;
            }
            if (persist && changed) {
                planDetailMapper.updateSchedule(detail);
            }
            index++;
        }
        details.sort(Comparator
                .comparing(PlanDetail::getScheduledDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PlanDetail::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PlanDetail::getDayNumber, Comparator.nullsLast(Integer::compareTo)));
    }

    private List<Map<String, Object>> buildScheduleConflicts(List<PlanDetail> details) {
        Map<LocalDate, List<PlanDetail>> byDate = new HashMap<>();
        for (PlanDetail detail : details) {
            if (detail.getScheduledDate() == null) {
                continue;
            }
            byDate.computeIfAbsent(detail.getScheduledDate(), key -> new ArrayList<>()).add(detail);
        }

        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map.Entry<LocalDate, List<PlanDetail>> entry : byDate.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("date", entry.getKey());
                conflict.put("count", entry.getValue().size());
                conflict.put("items", entry.getValue());
                conflicts.add(conflict);
            }
        }
        conflicts.sort(Comparator.comparing(item -> (LocalDate) item.get("date")));
        return conflicts;
    }

    private void createVersionSnapshot(StudyPlan plan, Long userId, String title) {
        List<PlanDetail> details = planDetailMapper.findByPlanId(plan.getId());
        normalizeSchedule(plan, details, false);

        PlanEditVersion version = new PlanEditVersion();
        version.setPlanId(plan.getId());
        version.setUserId(userId);
        version.setTitle(title);
        version.setSnapshot(JSON.toJSONString(details));
        planEditVersionMapper.insert(version);
        planEditVersionMapper.deleteOlderThanKeep(plan.getId(), 10);
    }

    private List<PlanDetail> refreshPlanRange(StudyPlan plan) {
        List<PlanDetail> details = planDetailMapper.findByPlanId(plan.getId());
        normalizeSchedule(plan, details, true);
        if (!details.isEmpty()) {
            LocalDate start = details.stream().map(PlanDetail::getScheduledDate).min(LocalDate::compareTo).orElse(plan.getStartDate());
            LocalDate end = details.stream().map(PlanDetail::getScheduledDate).max(LocalDate::compareTo).orElse(plan.getEndDate());
            plan.setStartDate(start);
            plan.setEndDate(end);
            plan.setTotalDays((int) (end.toEpochDay() - start.toEpochDay() + 1));
            planMapper.updateScheduleRange(plan);
        }
        return details;
    }

    private int calculateDayNumber(LocalDate startDate, LocalDate scheduledDate) {
        if (startDate == null || scheduledDate == null) {
            return 1;
        }
        return Math.max(1, (int) (scheduledDate.toEpochDay() - startDate.toEpochDay() + 1));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void ensurePlanWorkbenchSchema() {
        if (!columnExists("plan_detail", "scheduled_date")) {
            jdbcTemplate.execute("ALTER TABLE plan_detail ADD COLUMN scheduled_date DATE DEFAULT NULL COMMENT '任务排期日期' AFTER duration");
        }
        if (!columnExists("plan_detail", "sort_order")) {
            jdbcTemplate.execute("ALTER TABLE plan_detail ADD COLUMN sort_order INT DEFAULT NULL COMMENT '同一天内排序' AFTER scheduled_date");
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS plan_edit_version (
                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本ID',
                    plan_id BIGINT NOT NULL COMMENT '计划ID',
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    title VARCHAR(100) NOT NULL COMMENT '版本标题',
                    snapshot JSON NOT NULL COMMENT '任务快照',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    PRIMARY KEY (id),
                    KEY idx_plan_edit_version_plan (plan_id),
                    KEY idx_plan_edit_version_user (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划编辑版本快照表'
                """);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private StudyPlan validatePlanOwner(Long planId, Long userId) {
        StudyPlan plan = planMapper.findById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan does not exist");
        }

        if (userId != null && !plan.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to access this plan");
        }

        return plan;
    }

    private StudyPlan validatePlanMember(Long planId, Long userId) {
        StudyPlan plan = planMapper.findById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan does not exist");
        }
        if (userId != null && !planGroupService.canAccessPlan(planId, userId)) {
            throw new RuntimeException("No permission to access this plan");
        }
        return plan;
    }

    private StudyPlan validatePlanManager(Long planId, Long userId) {
        StudyPlan plan = planMapper.findById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan does not exist");
        }
        if (userId != null && !planGroupService.canManagePlan(planId, userId)) {
            throw new RuntimeException("No permission to manage this plan");
        }
        return plan;
    }
}
