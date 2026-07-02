package com.studyplanner.service;

import com.studyplanner.entity.CheckIn;
import com.studyplanner.entity.PlanDetail;
import com.studyplanner.entity.PlanMember;
import com.studyplanner.entity.StudyPlan;
import com.studyplanner.mapper.CheckInMapper;
import com.studyplanner.mapper.PlanDetailMapper;
import com.studyplanner.mapper.PlanMapper;
import com.studyplanner.mapper.PlanMemberMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlanGroupService {
    private static final String TYPE_PERSONAL = "personal";
    private static final String TYPE_GROUP = "group";
    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_MEMBER = "member";
    private static final String STATUS_ACTIVE = "active";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PlanMapper planMapper;
    private final PlanDetailMapper planDetailMapper;
    private final PlanMemberMapper planMemberMapper;
    private final CheckInMapper checkInMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    public PlanGroupService(
            PlanMapper planMapper,
            PlanDetailMapper planDetailMapper,
            PlanMemberMapper planMemberMapper,
            CheckInMapper checkInMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.planMapper = planMapper;
        this.planDetailMapper = planDetailMapper;
        this.planMemberMapper = planMemberMapper;
        this.checkInMapper = checkInMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureSchema() {
        if (!columnExists("study_plan", "plan_type")) {
            jdbcTemplate.execute("ALTER TABLE study_plan ADD COLUMN plan_type VARCHAR(20) NOT NULL DEFAULT 'personal' AFTER status");
        }
        if (!columnExists("study_plan", "invite_code")) {
            jdbcTemplate.execute("ALTER TABLE study_plan ADD COLUMN invite_code VARCHAR(16) DEFAULT NULL AFTER plan_type");
        }
        if (!columnExists("study_plan", "source_group_plan_id")) {
            jdbcTemplate.execute("ALTER TABLE study_plan ADD COLUMN source_group_plan_id BIGINT DEFAULT NULL AFTER invite_code");
        }
        planMemberMapper.createTable();
    }

    @Transactional
    public Map<String, Object> enableGroupPlan(Long planId, Long userId) {
        ensureSchema();
        StudyPlan plan = requireOwner(planId, userId);
        String inviteCode = hasText(plan.getInviteCode()) ? plan.getInviteCode() : generateInviteCode();
        planMapper.updateGroupSettings(planId, TYPE_GROUP, inviteCode);
        ensureOwnerMember(planId, userId);
        return getGroupSummary(planId, userId);
    }

    @Transactional
    public Map<String, Object> joinPlan(Long planId, Long userId, String inviteCode) {
        ensureSchema();
        StudyPlan plan = requirePlan(planId);
        if (!isGroupPlan(plan)) {
            throw new IllegalArgumentException("This plan is not open for group study");
        }
        if (plan.getUserId() != null && plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能通过自己的邀请码加入自己创建的小组");
        }
        if (hasText(plan.getInviteCode()) && !plan.getInviteCode().equalsIgnoreCase(nullToEmpty(inviteCode).trim())) {
            throw new IllegalArgumentException("Invalid invite code");
        }
        addMember(planId, userId, ROLE_MEMBER);
        return getGroupSummary(planId, userId);
    }

    @Transactional
    public Map<String, Object> joinPlanByInviteCode(Long userId, String inviteCode) {
        ensureSchema();
        String normalizedCode = nullToEmpty(inviteCode).trim();
        if (!hasText(normalizedCode)) {
            throw new IllegalArgumentException("请输入邀请码");
        }

        StudyPlan groupPlan = planMapper.findByInviteCode(normalizedCode);
        if (groupPlan == null) {
            throw new IllegalArgumentException("邀请码不存在");
        }
        if (groupPlan.getUserId() != null && groupPlan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能通过自己的邀请码加入自己创建的小组");
        }

        Map<String, Object> summary = joinPlan(groupPlan.getId(), userId, normalizedCode);
        StudyPlan personalPlan = clonePlanForMember(groupPlan, userId);
        summary.put("planId", groupPlan.getId());
        summary.put("planTitle", groupPlan.getTitle());
        summary.put("personalPlanId", personalPlan.getId());
        return summary;
    }

    @Transactional
    public Map<String, Object> leavePlan(Long planId, Long userId, boolean deleteLinkedPlan) {
        ensureSchema();
        PlanMember member = planMemberMapper.findByPlanIdAndUserId(planId, userId);
        if (member == null || !STATUS_ACTIVE.equals(member.getStatus())) {
            throw new IllegalArgumentException("You are not a member of this plan");
        }
        if (ROLE_OWNER.equals(member.getRole())) {
            throw new IllegalArgumentException("Owner cannot leave the group plan");
        }

        planMemberMapper.removeMember(planId, userId);

        StudyPlan linkedPlan = planMapper.findPersonalCopyBySourceGroupPlanId(userId, planId);
        Long deletedPlanId = null;
        if (deleteLinkedPlan && linkedPlan != null) {
            deletedPlanId = linkedPlan.getId();
            planMapper.delete(linkedPlan.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("leftPlanId", planId);
        result.put("deletedPersonalPlanId", deletedPlanId);
        result.put("keptPersonalPlanId", !deleteLinkedPlan && linkedPlan != null ? linkedPlan.getId() : null);
        return result;
    }

    @Transactional
    public Map<String, Object> leavePlan(Long planId, Long userId) {
        return leavePlan(planId, userId, false);
    }

    public Map<String, Object> getGroupSummary(Long planId, Long userId) {
        ensureSchema();
        StudyPlan plan = requirePlan(planId);
        boolean owner = plan.getUserId().equals(userId);
        PlanMember currentMember = planMemberMapper.findByPlanIdAndUserId(planId, userId);
        boolean member = currentMember != null && STATUS_ACTIVE.equals(currentMember.getStatus());
        if (!owner && !member && !isGroupPlan(plan)) {
            throw new RuntimeException("No permission to access this plan");
        }
        if (owner) {
            ensureOwnerMember(planId, userId);
        }

        List<Map<String, Object>> members = buildMemberProgress(planId);
        StudyPlan linkedPlan = planMapper.findPersonalCopyBySourceGroupPlanId(userId, planId);

        Map<String, Object> result = new HashMap<>();
        result.put("planType", defaultType(plan.getPlanType()));
        result.put("planId", plan.getId());
        result.put("planTitle", plan.getTitle());
        result.put("inviteCode", plan.getInviteCode());
        result.put("isGroupPlan", isGroupPlan(plan));
        result.put("isOwner", owner);
        result.put("isMember", owner || member);
        result.put("memberCount", members.size());
        result.put("members", members);
        result.put("currentRole", owner ? ROLE_OWNER : currentMember != null ? currentMember.getRole() : null);
        result.put("linkedPersonalPlanId", linkedPlan != null ? linkedPlan.getId() : null);
        return result;
    }

    public List<Map<String, Object>> getMyGroupPlans(Long userId) {
        ensureSchema();
        return planMapper.findGroupPlansByMember(userId).stream().map(row -> {
            Long planId = toLong(row.get("id"));
            int totalTasks = planDetailMapper.findByPlanId(planId).size();
            long completedTasks = checkInMapper.findByPlanIdAndUserId(planId, userId).stream()
                    .map(CheckIn::getDetailId)
                    .distinct()
                    .count();
            row.put("completed_tasks", completedTasks);
            row.put("total_tasks", totalTasks);
            row.put("progress", totalTasks == 0 ? 0 : completedTasks * 100.0 / totalTasks);
            return row;
        }).toList();
    }

    public boolean canAccessPlan(Long planId, Long userId) {
        ensureSchema();
        StudyPlan plan = planMapper.findById(planId);
        if (plan == null || userId == null) {
            return false;
        }
        if (plan.getUserId().equals(userId)) {
            ensureOwnerMember(planId, userId);
            return true;
        }
        PlanMember member = planMemberMapper.findByPlanIdAndUserId(planId, userId);
        return member != null && STATUS_ACTIVE.equals(member.getStatus());
    }

    public boolean canManagePlan(Long planId, Long userId) {
        ensureSchema();
        StudyPlan plan = planMapper.findById(planId);
        if (plan == null || userId == null) {
            return false;
        }
        if (plan.getUserId().equals(userId)) {
            return true;
        }
        PlanMember member = planMemberMapper.findByPlanIdAndUserId(planId, userId);
        return member != null && STATUS_ACTIVE.equals(member.getStatus())
                && ("admin".equals(member.getRole()) || ROLE_OWNER.equals(member.getRole()));
    }

    public boolean isGroupPlan(Long planId) {
        ensureSchema();
        StudyPlan plan = planMapper.findById(planId);
        return plan != null && isGroupPlan(plan);
    }

    @Transactional
    public void ensureOwnerMember(Long planId, Long userId) {
        addMember(planId, userId, ROLE_OWNER);
    }

    private List<Map<String, Object>> buildMemberProgress(Long planId) {
        int totalTasks = planDetailMapper.findByPlanId(planId).size();
        return planMemberMapper.findActiveByPlanId(planId).stream().map(member -> {
            List<CheckIn> checkIns = checkInMapper.findByPlanIdAndUserId(planId, member.getUserId());
            long completedTasks = checkIns.stream()
                    .map(CheckIn::getDetailId)
                    .distinct()
                    .count();
            double totalHours = checkIns.stream()
                    .filter(item -> item.getStudyHours() != null)
                    .mapToDouble(item -> item.getStudyHours().doubleValue())
                    .sum();

            Map<String, Object> row = new HashMap<>();
            row.put("userId", member.getUserId());
            row.put("username", member.getUsername());
            row.put("avatar", member.getAvatar());
            row.put("role", member.getRole());
            row.put("joinedAt", member.getJoinedAt());
            row.put("completedTasks", completedTasks);
            row.put("totalTasks", totalTasks);
            row.put("progress", totalTasks == 0 ? 0 : completedTasks * 100.0 / totalTasks);
            row.put("studyHours", totalHours);
            return row;
        }).toList();
    }

    private void addMember(Long planId, Long userId, String role) {
        PlanMember member = new PlanMember();
        member.setPlanId(planId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(STATUS_ACTIVE);
        planMemberMapper.insertOrActivate(member);
    }

    private StudyPlan clonePlanForMember(StudyPlan sourcePlan, Long userId) {
        StudyPlan existing = planMapper.findPersonalCopyBySourceGroupPlanId(userId, sourcePlan.getId());
        if (existing != null) {
            return existing;
        }

        List<PlanDetail> sourceDetails = planDetailMapper.findByPlanId(sourcePlan.getId());
        LocalDate startDate = LocalDate.now();
        int totalDays = sourcePlan.getTotalDays() != null && sourcePlan.getTotalDays() > 0
                ? sourcePlan.getTotalDays()
                : Math.max(sourceDetails.size(), 1);

        StudyPlan copiedPlan = new StudyPlan();
        copiedPlan.setUserId(userId);
        copiedPlan.setTitle(sourcePlan.getTitle());
        copiedPlan.setGoal(sourcePlan.getGoal());
        copiedPlan.setLevel(sourcePlan.getLevel());
        copiedPlan.setDailyHours(sourcePlan.getDailyHours());
        copiedPlan.setTotalDays(totalDays);
        copiedPlan.setStartDate(startDate);
        copiedPlan.setEndDate(startDate.plusDays(Math.max(totalDays - 1L, 0L)));
        copiedPlan.setStatus(STATUS_ACTIVE);
        copiedPlan.setPlanType(TYPE_PERSONAL);
        copiedPlan.setInviteCode(null);
        copiedPlan.setSourceGroupPlanId(sourcePlan.getId());
        planMapper.insert(copiedPlan);

        int order = 1;
        for (PlanDetail sourceDetail : sourceDetails) {
            PlanDetail copiedDetail = new PlanDetail();
            copiedDetail.setPlanId(copiedPlan.getId());
            copiedDetail.setDayNumber(sourceDetail.getDayNumber() != null ? sourceDetail.getDayNumber() : order);
            copiedDetail.setContent(sourceDetail.getContent());
            copiedDetail.setDuration(sourceDetail.getDuration());
            copiedDetail.setScheduledDate(startDate.plusDays(Math.max(copiedDetail.getDayNumber() - 1L, 0L)));
            copiedDetail.setSortOrder(sourceDetail.getSortOrder() != null ? sourceDetail.getSortOrder() : order);
            copiedDetail.setIsCompleted(0);
            planDetailMapper.insert(copiedDetail);
            order++;
        }
        return copiedPlan;
    }

    private StudyPlan requireOwner(Long planId, Long userId) {
        StudyPlan plan = requirePlan(planId);
        if (!plan.getUserId().equals(userId)) {
            throw new RuntimeException("Only the owner can manage group settings");
        }
        return plan;
    }

    private StudyPlan requirePlan(Long planId) {
        StudyPlan plan = planMapper.findById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan does not exist");
        }
        return plan;
    }

    private boolean isGroupPlan(StudyPlan plan) {
        return TYPE_GROUP.equals(defaultType(plan.getPlanType()))
                || "invite_only".equals(defaultType(plan.getPlanType()))
                || "public".equals(defaultType(plan.getPlanType()));
    }

    private String defaultType(String type) {
        return hasText(type) ? type : TYPE_PERSONAL;
    }

    private String generateInviteCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return builder.toString().toUpperCase(Locale.ROOT);
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }
}
