package com.studyplanner.mapper;

import com.studyplanner.entity.PlanMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PlanMemberMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS plan_member (
                id BIGINT NOT NULL AUTO_INCREMENT,
                plan_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                role VARCHAR(20) NOT NULL DEFAULT 'member',
                status VARCHAR(20) NOT NULL DEFAULT 'active',
                joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_plan_member (plan_id, user_id),
                KEY idx_plan_member_plan (plan_id),
                KEY idx_plan_member_user (user_id),
                CONSTRAINT fk_plan_member_plan FOREIGN KEY (plan_id) REFERENCES study_plan(id) ON DELETE CASCADE,
                CONSTRAINT fk_plan_member_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createTable();

    @Insert("""
            INSERT INTO plan_member (plan_id, user_id, role, status)
            VALUES (#{planId}, #{userId}, #{role}, #{status})
            ON DUPLICATE KEY UPDATE status = VALUES(status), role = IF(role = 'owner', role, VALUES(role))
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrActivate(PlanMember member);

    @Select("""
            SELECT pm.*, u.username, u.avatar
            FROM plan_member pm
            JOIN user u ON u.id = pm.user_id
            WHERE pm.plan_id = #{planId} AND pm.status = 'active'
            ORDER BY FIELD(pm.role, 'owner', 'admin', 'member'), pm.joined_at
            """)
    List<PlanMember> findActiveByPlanId(Long planId);

    @Select("SELECT * FROM plan_member WHERE plan_id = #{planId} AND user_id = #{userId} LIMIT 1")
    PlanMember findByPlanIdAndUserId(@Param("planId") Long planId, @Param("userId") Long userId);

    @Update("UPDATE plan_member SET status = 'removed' WHERE plan_id = #{planId} AND user_id = #{userId} AND role <> 'owner'")
    int removeMember(@Param("planId") Long planId, @Param("userId") Long userId);

    @Delete("DELETE FROM plan_member WHERE plan_id = #{planId}")
    int deleteByPlanId(Long planId);
}
