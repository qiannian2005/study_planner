import request from "./request";

export const planApi = {
  // 生成学习计划
  generatePlan(data) {
    return request({
      url: "/plan/generate",
      method: "POST",
      data,
    });
  },

  analyzeGoal(data) {
    return request({
      url: "/plan/analyze-goal",
      method: "POST",
      data,
    });
  },

  optimizeGoal(data) {
    return request({
      url: "/plan/optimize-goal",
      method: "POST",
      data,
    });
  },

  generateRoadmap(data) {
    return request({
      url: "/plan/generate-roadmap",
      method: "POST",
      data,
    });
  },

  askDraftAdvisor(data) {
    return request({
      url: "/plan/advisor/draft",
      method: "POST",
      data,
    });
  },

  askPlanAdvisor(planId, data) {
    return request({
      url: `/plan/${planId}/advisor`,
      method: "POST",
      data,
    });
  },

  previewAdjustment(planId, data) {
    return request({
      url: `/plan/${planId}/adjustment/preview`,
      method: "POST",
      data,
    });
  },

  applyAdjustment(planId, data) {
    return request({
      url: `/plan/${planId}/adjustment/apply`,
      method: "POST",
      data,
    });
  },

  // 获取计划列表
  getPlans() {
    return request({
      url: "/plan/list",
      method: "GET",
    });
  },

  // 获取计划详情
  getPlanDetail(planId) {
    return request({
      url: `/plan/${planId}`,
      method: "GET",
    });
  },

  // 获取今日任务
  getTodayTask(planId) {
    return request({
      url: `/plan/${planId}/today`,
      method: "GET",
    });
  },

  // 删除计划
  deletePlan(planId) {
    return request({
      url: `/plan/${planId}`,
      method: "DELETE",
    });
  },

  // 更新计划
  updatePlan(planId, data) {
    return request({
      url: `/plan/${planId}`,
      method: "PUT",
      data,
    });
  },

  getSchedule(planId) {
    return request({
      url: `/plan/${planId}/schedule`,
      method: "GET",
    });
  },

  updateSchedule(planId, data) {
    return request({
      url: `/plan/${planId}/schedule`,
      method: "PUT",
      data,
    });
  },

  batchUpdateTasks(planId, data) {
    return request({
      url: `/plan/${planId}/details/batch`,
      method: "PUT",
      data,
    });
  },

  addTask(planId, data) {
    return request({
      url: `/plan/${planId}/details`,
      method: "POST",
      data,
    });
  },

  deleteTask(planId, detailId) {
    return request({
      url: `/plan/${planId}/details/${detailId}`,
      method: "DELETE",
    });
  },

  getVersions(planId) {
    return request({
      url: `/plan/${planId}/versions`,
      method: "GET",
    });
  },

  restoreVersion(planId, versionId) {
    return request({
      url: `/plan/${planId}/versions/${versionId}/restore`,
      method: "POST",
    });
  },

  getGroupSummary(planId) {
    return request({
      url: `/plan/${planId}/group`,
      method: "GET",
    });
  },

  enableGroupPlan(planId) {
    return request({
      url: `/plan/${planId}/group/enable`,
      method: "POST",
    });
  },

  joinGroupPlan(planId, inviteCode) {
    return request({
      url: `/plan/${planId}/group/join`,
      method: "POST",
      data: { inviteCode },
    });
  },

  joinGroupPlanByCode(inviteCode) {
    return request({
      url: "/plan/group/join-by-code",
      method: "POST",
      data: { inviteCode },
    });
  },

  leaveGroupPlan(planId, data = {}) {
    return request({
      url: `/plan/${planId}/group/leave`,
      method: "POST",
      data,
    });
  },

  getMyGroupPlans() {
    return request({
      url: "/plan/group/my",
      method: "GET",
    });
  },
};
