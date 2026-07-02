package com.studyplanner.mapper;

import com.studyplanner.entity.PlanEditVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlanEditVersionMapper {

    @Insert("INSERT INTO plan_edit_version (plan_id, user_id, title, snapshot, create_time) " +
            "VALUES (#{planId}, #{userId}, #{title}, #{snapshot}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanEditVersion version);

    @Select("SELECT * FROM plan_edit_version WHERE plan_id = #{planId} ORDER BY create_time DESC")
    List<PlanEditVersion> findByPlanId(Long planId);

    @Select("SELECT * FROM plan_edit_version WHERE id = #{id} AND plan_id = #{planId}")
    PlanEditVersion findByIdAndPlanId(@Param("id") Long id, @Param("planId") Long planId);

    @Delete("DELETE FROM plan_edit_version WHERE plan_id = #{planId} AND id NOT IN (" +
            "SELECT id FROM (SELECT id FROM plan_edit_version WHERE plan_id = #{planId} ORDER BY create_time DESC LIMIT #{keep}) t)")
    int deleteOlderThanKeep(@Param("planId") Long planId, @Param("keep") int keep);
}
