package com.studyplanner.mapper;

import com.studyplanner.entity.StudyPlan;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * 学习计划Mapper接口
 */
@Mapper
public interface PlanMapper {

    /**
     * 根据ID查询计划
     */
    @Select("SELECT * FROM study_plan WHERE id = #{id}")
    StudyPlan findById(Long id);

    @Select("SELECT * FROM study_plan WHERE UPPER(invite_code) = UPPER(#{inviteCode}) LIMIT 1")
    StudyPlan findByInviteCode(String inviteCode);

    /**
     * 查询用户的所有计划
     */
    @Select("SELECT * FROM study_plan WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<StudyPlan> findByUserId(Long userId);

    /**
     * 查询用户进行中的计划
     */
    @Select("SELECT * FROM study_plan WHERE user_id = #{userId} AND status = '进行中' ORDER BY create_time DESC")
    List<StudyPlan> findActiveByUserId(Long userId);
    
    /**
     * 查询所有进行中的计划（用于定时任务）
     */
    @Select("SELECT * FROM study_plan WHERE status = '进行中'")
    List<StudyPlan> findAllActivePlans();

    /**
     * 插入新计划
     */
    @Insert("INSERT INTO study_plan (user_id, title, goal, level, daily_hours, total_days, start_date, end_date, status, plan_type, invite_code, source_group_plan_id, create_time, update_time) "
            +
            "VALUES (#{userId}, #{title}, #{goal}, #{level}, #{dailyHours}, #{totalDays}, #{startDate}, #{endDate}, #{status}, #{planType}, #{inviteCode}, #{sourceGroupPlanId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyPlan plan);

    /**
     * 更新计划
     */
    @Update("UPDATE study_plan SET title = #{title}, goal = #{goal}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(StudyPlan plan);

    /**
     * 更新计划状态
     */
    @Update("UPDATE study_plan SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE study_plan SET total_days = #{totalDays}, start_date = #{startDate}, end_date = #{endDate}, update_time = NOW() WHERE id = #{id}")
    int updateScheduleRange(StudyPlan plan);

    @Update("UPDATE study_plan SET plan_type = #{planType}, invite_code = #{inviteCode}, update_time = NOW() WHERE id = #{id}")
    int updateGroupSettings(@Param("id") Long id, @Param("planType") String planType, @Param("inviteCode") String inviteCode);

    @Select("""
            SELECT sp.*, pm.role AS member_role, pm.joined_at,
                   COUNT(active_pm.id) AS member_count,
                   (
                       SELECT copy_plan.id
                       FROM study_plan copy_plan
                       WHERE copy_plan.user_id = #{userId}
                         AND copy_plan.source_group_plan_id = sp.id
                       ORDER BY copy_plan.create_time DESC
                       LIMIT 1
                   ) AS linked_personal_plan_id
            FROM study_plan sp
            JOIN plan_member pm ON pm.plan_id = sp.id
                AND pm.user_id = #{userId}
                AND pm.status = 'active'
            LEFT JOIN plan_member active_pm ON active_pm.plan_id = sp.id
                AND active_pm.status = 'active'
            WHERE sp.plan_type IN ('group', 'invite_only', 'public')
            GROUP BY sp.id, pm.role, pm.joined_at
            ORDER BY sp.update_time DESC, sp.create_time DESC
            """)
    List<Map<String, Object>> findGroupPlansByMember(Long userId);

    @Select("""
            SELECT *
            FROM study_plan
            WHERE user_id = #{userId}
              AND source_group_plan_id = #{sourceGroupPlanId}
            ORDER BY create_time DESC
            LIMIT 1
            """)
    StudyPlan findPersonalCopyBySourceGroupPlanId(@Param("userId") Long userId, @Param("sourceGroupPlanId") Long sourceGroupPlanId);

    /**
     * 删除计划
     */
    @Delete("DELETE FROM study_plan WHERE id = #{id}")
    int delete(Long id);
}
