package com.studyplanner.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.studyplanner.dto.ApiResponse;
import com.studyplanner.dto.PlanGenerateRequest;
import com.studyplanner.dto.PlanTaskEditRequest;
import com.studyplanner.entity.CheckIn;
import com.studyplanner.entity.PlanDetail;
import com.studyplanner.entity.PlanEditVersion;
import com.studyplanner.entity.StudyPlan;
import com.studyplanner.mapper.CheckInMapper;
import com.studyplanner.service.LLMService;
import com.studyplanner.service.PlanGroupService;
import com.studyplanner.service.PlanService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    @Autowired
    private PlanService planService;

    @Autowired
    private LLMService llmService;

    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private PlanGroupService planGroupService;

    @GetMapping("/models")
    public ApiResponse<Map<String, Object>> getAvailableModels(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        Map<String, Object> result = new HashMap<>();
        result.put("models", llmService.getAvailableModels());
        result.put("defaultModel", llmService.getDefaultModel());
        result.put("isLoggedIn", userId != null);

        return ApiResponse.success(result);
    }

    @PostMapping("/analyze-goal")
    public ApiResponse<JSONObject> analyzeGoal(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String goal = readString(request, "goal");
        if (goal == null || goal.trim().isEmpty()) {
            return ApiResponse.badRequest("学习目标不能为空");
        }

        String level = readString(request, "level");
        JSONObject analysis = llmService.analyzeGoal(goal, level);
        return ApiResponse.success(analysis);
    }

    @PostMapping("/optimize-goal")
    public ApiResponse<JSONObject> optimizeGoal(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String goal = readString(request, "goal");
        if (goal == null || goal.trim().isEmpty()) {
            return ApiResponse.badRequest("学习目标不能为空");
        }

        String level = readString(request, "level");
        Integer totalDays = readInteger(request, "totalDays");
        Double dailyHours = readDouble(request, "dailyHours");
        JSONObject optimizedGoal = llmService.optimizeGoal(goal, level, totalDays, dailyHours);
        return ApiResponse.success(optimizedGoal);
    }

    @PostMapping("/generate-roadmap")
    public ApiResponse<JSONArray> generateRoadmap(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String goal = readString(request, "goal");
        if (goal == null || goal.trim().isEmpty()) {
            return ApiResponse.badRequest("学习目标不能为空");
        }

        Integer totalDays = readInteger(request, "totalDays");
        if (totalDays == null || totalDays < 1) {
            return ApiResponse.badRequest("计划天数不能为空");
        }
        if (totalDays > 30) {
            return ApiResponse.badRequest("计划天数不能超过30天");
        }

        String level = readString(request, "level");
        Double dailyHours = readDouble(request, "dailyHours");
        if (dailyHours == null || dailyHours < 0.5 || dailyHours > 4) {
            return ApiResponse.badRequest("每日学习时间只能在0.5-4小时之间");
        }
        String studyMode = readString(request, "studyMode");
        String studyPreference = readString(request, "studyPreference");
        @SuppressWarnings("unchecked")
        Map<String, Object> goalAnalysis = request.get("goalAnalysis") instanceof Map<?, ?>
                ? (Map<String, Object>) request.get("goalAnalysis")
                : null;

        JSONArray roadmap = llmService.generateRoadmap(goal, level, totalDays, dailyHours, studyMode, studyPreference, goalAnalysis);
        return ApiResponse.success(roadmap);
    }

    @PostMapping("/generate")
    public ApiResponse<StudyPlan> generatePlan(@Valid @RequestBody PlanGenerateRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            StudyPlan plan = planService.generatePlan(userId, request);
            return ApiResponse.success("计划生成成功", plan);
        } catch (Exception e) {
            return ApiResponse.error("计划生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/generate/guest")
    public ApiResponse<JSONObject> generatePlanForGuest(@RequestBody PlanGenerateRequest request) {
        if (request.getGoal() == null || request.getGoal().trim().isEmpty()) {
            return ApiResponse.error("学习目标不能为空");
        }
        if (request.getCustomApiUrl() == null || request.getCustomApiUrl().trim().isEmpty()) {
            return ApiResponse.error("API URL不能为空");
        }
        if (request.getCustomApiKey() == null || request.getCustomApiKey().trim().isEmpty()) {
            return ApiResponse.error("API Key不能为空");
        }
        if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
            return ApiResponse.error("模型名称不能为空");
        }

        try {
            JSONObject plan = planService.generatePlanForGuest(request);
            return ApiResponse.success("计划生成成功（游客预览）", plan);
        } catch (Exception e) {
            return ApiResponse.error("计划生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/advisor/draft")
    public ApiResponse<Map<String, Object>> askDraftAdvisor(@RequestBody Map<String, Object> request,
                                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            return ApiResponse.success(planService.askDraftAdvisor(userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error("AI学习顾问调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/advisor")
    public ApiResponse<Map<String, Object>> askPlanAdvisor(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> request,
                                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            return ApiResponse.success(planService.askPlanAdvisor(id, userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error("AI学习顾问调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/adjustment/preview")
    public ApiResponse<JSONObject> previewAdaptiveAdjustment(@PathVariable Long id,
                                                             @RequestBody Map<String, Object> request,
                                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            return ApiResponse.success(planService.previewAdaptiveAdjustment(id, userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error("AI调整建议生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/adjustment/apply")
    public ApiResponse<List<PlanDetail>> applyAdaptiveAdjustment(@PathVariable Long id,
                                                                 @RequestBody Map<String, Object> request,
                                                                 HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            return ApiResponse.success("AI调整已应用", planService.applyAdaptiveAdjustment(id, userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error("AI调整应用失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<StudyPlan>> getUserPlans(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        List<StudyPlan> plans = planService.getUserPlans(userId);
        return ApiResponse.success(plans);
    }

    @GetMapping("/my-plans")
    public ApiResponse<List<StudyPlan>> getMyPlans(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        List<StudyPlan> plans = planService.getUserPlans(userId);
        return ApiResponse.success(plans);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getPlanDetail(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        StudyPlan plan = planService.getPlanWithDetails(id);
        if (plan == null) {
            return ApiResponse.error("计划不存在");
        }

        if (!planGroupService.canAccessPlan(id, userId)) {
            return ApiResponse.error("无权访问该计划");
        }

        List<PlanDetail> details = planService.getPlanDetails(id);
        List<CheckIn> checkIns = checkInMapper.findByPlanIdAndUserId(id, userId);
        Map<Long, CheckIn> checkInByDetailId = new HashMap<>();
        for (CheckIn checkIn : checkIns) {
            checkInByDetailId.put(checkIn.getDetailId(), checkIn);
        }
        Map<String, Object> groupSummary = planGroupService.getGroupSummary(id, userId);
        boolean groupPlan = Boolean.TRUE.equals(groupSummary.get("isGroupPlan"));

        List<Map<String, Object>> detailItems = details.stream().map(detail -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", detail.getId());
            item.put("planId", detail.getPlanId());
            item.put("dayNumber", detail.getDayNumber());
            item.put("content", detail.getContent());
            item.put("duration", detail.getDuration());
            item.put("scheduledDate", detail.getScheduledDate());
            item.put("sortOrder", detail.getSortOrder());
            item.put("createTime", detail.getCreateTime());
            CheckIn checkIn = checkInByDetailId.get(detail.getId());
            item.put("isCompleted", groupPlan ? (checkIn != null ? 1 : 0) : detail.getIsCompleted());
            if (checkIn != null) {
                item.put("checkInDate", checkIn.getCheckDate());
                item.put("studyHours", checkIn.getStudyHours());
                item.put("checkInTime", checkIn.getCreateTime());
            }
            return item;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("details", detailItems);
        result.put("progress", groupPlan && !details.isEmpty()
                ? checkInByDetailId.keySet().size() * 100.0 / details.size()
                : planService.getPlanProgress(id));
        result.put("group", groupSummary);

        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/group")
    public ApiResponse<Map<String, Object>> getGroupSummary(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        try {
            return ApiResponse.success(planGroupService.getGroupSummary(id, userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/group/enable")
    public ApiResponse<Map<String, Object>> enableGroupPlan(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        try {
            return ApiResponse.success("小组计划已开启", planGroupService.enableGroupPlan(id, userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/group/join")
    public ApiResponse<Map<String, Object>> joinGroupPlan(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request,
                                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        try {
            return ApiResponse.success("已加入小组计划", planGroupService.joinPlan(id, userId, request.get("inviteCode")));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/group/join-by-code")
    public ApiResponse<Map<String, Object>> joinGroupPlanByCode(@RequestBody Map<String, String> request,
                                                                HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        try {
            return ApiResponse.success("已加入小组计划", planGroupService.joinPlanByInviteCode(userId, request.get("inviteCode")));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/group/leave")
    public ApiResponse<Map<String, Object>> leaveGroupPlan(@PathVariable Long id,
                                                           @RequestBody(required = false) Map<String, Object> request,
                                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        try {
            return ApiResponse.success("已退出小组计划", planGroupService.leavePlan(id, userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/group/my")
    public ApiResponse<List<Map<String, Object>>> getMyGroupPlans(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        try {
            return ApiResponse.success(planGroupService.getMyGroupPlans(userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/today")
    public ApiResponse<PlanDetail> getTodayTask(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            PlanDetail task = planService.getTodayTask(id, userId);
            if (task == null) {
                return ApiResponse.error("今日没有任务或计划已结束");
            }

            return ApiResponse.success(task);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/schedule")
    public ApiResponse<Map<String, Object>> getPlanSchedule(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            return ApiResponse.success(planService.getPlanSchedule(id, userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/schedule")
    public ApiResponse<List<PlanDetail>> updatePlanSchedule(@PathVariable Long id,
                                                            @RequestBody List<PlanTaskEditRequest> request,
                                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            return ApiResponse.success("排期已保存", planService.updateTaskSchedule(id, userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/details/batch")
    public ApiResponse<List<PlanDetail>> batchUpdateTasks(@PathVariable Long id,
                                                          @RequestBody List<PlanTaskEditRequest> request,
                                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            return ApiResponse.success("任务已保存", planService.batchUpdateTasks(id, userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/details")
    public ApiResponse<PlanDetail> addTask(@PathVariable Long id,
                                           @RequestBody PlanTaskEditRequest request,
                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            return ApiResponse.success("任务已新增", planService.addTask(id, userId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/details/{detailId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id,
                                        @PathVariable Long detailId,
                                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            planService.deleteTask(id, detailId, userId);
            return ApiResponse.success("任务已删除", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<PlanEditVersion>> getPlanVersions(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            return ApiResponse.success(planService.getVersions(id, userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public ApiResponse<List<PlanDetail>> restorePlanVersion(@PathVariable Long id,
                                                            @PathVariable Long versionId,
                                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }

        try {
            return ApiResponse.success("版本已回滚", planService.restoreVersion(id, versionId, userId));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updatePlanStatus(@PathVariable Long id, @RequestBody Map<String, String> request,
                                              HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String status = request.get("status");
        planService.updatePlanStatus(id, status);
        return ApiResponse.success("状态更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            planService.deletePlan(id, userId);
            return ApiResponse.success("计划删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updatePlan(@PathVariable Long id, @RequestBody Map<String, String> request,
                                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        try {
            planService.updatePlan(id, userId, request.get("title"), request.get("goal"));
            return ApiResponse.success("计划更新成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private String readString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer readInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && !value.toString().trim().isEmpty()) {
            return Integer.parseInt(value.toString());
        }
        return null;
    }

    private Double readDouble(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null && !value.toString().trim().isEmpty()) {
            return Double.parseDouble(value.toString());
        }
        return null;
    }
}
