package com.studyplanner.mapper;

import com.studyplanner.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    @Update("ALTER TABLE user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' AFTER avatar")
    void addRoleColumn();

    @Update("ALTER TABLE user ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER role")
    void addStatusColumn();
    
    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);
    
    /**
     * 插入新用户
     */
    @Insert("INSERT INTO user (username, password, email, avatar, create_time, update_time) " +
            "VALUES (#{username}, #{password}, #{email}, #{avatar}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    /**
     * 更新用户信息
     */
    @Update("UPDATE user SET email = #{email}, avatar = #{avatar}, update_time = NOW() WHERE id = #{id}")
    int update(User user);
    
    /**
     * 更新头像
     */
    @Update("UPDATE user SET avatar = #{avatar}, update_time = NOW() WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);
    
    /**
     * 更新密码
     */
    @Update("UPDATE user SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("""
            SELECT u.id, u.username, u.email, u.avatar, u.role, u.status,
                   u.create_time AS created_at, u.update_time AS updated_at,
                   (SELECT COUNT(*) FROM study_plan sp WHERE sp.user_id = u.id) AS plan_count,
                   (SELECT COUNT(*) FROM forum_question q WHERE q.user_id = u.id) AS question_count,
                   (SELECT COUNT(*) FROM forum_answer a WHERE a.user_id = u.id) AS answer_count
            FROM user u
            WHERE (#{keyword} IS NULL OR #{keyword} = ''
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.email LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY u.create_time DESC
            """)
    List<Map<String, Object>> findAdminUsers(@Param("keyword") String keyword);

    @Update("""
            UPDATE user
            SET status = #{status}, update_time = NOW()
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
