package com.studyplanner.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatRoomMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS chat_room_message (
                id BIGINT NOT NULL AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                room_type VARCHAR(20) NOT NULL DEFAULT 'global',
                room_id BIGINT DEFAULT NULL,
                content TEXT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_chat_room_user (user_id),
                KEY idx_chat_room_scope (room_type, room_id),
                KEY idx_chat_room_create_time (create_time),
                CONSTRAINT fk_chat_room_message_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createMessageTable();

    @Update("ALTER TABLE chat_room_message ADD COLUMN room_type VARCHAR(20) NOT NULL DEFAULT 'global' AFTER user_id")
    void addRoomTypeColumn();

    @Update("ALTER TABLE chat_room_message ADD COLUMN room_id BIGINT DEFAULT NULL AFTER room_type")
    void addRoomIdColumn();

    @Insert("""
            INSERT INTO chat_room_message (user_id, room_type, room_id, content)
            VALUES (#{userId}, #{roomType}, #{roomId}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(Map<String, Object> message);

    @Select("""
            SELECT m.id, m.user_id, m.room_type, m.room_id, u.username, u.avatar, m.content, m.create_time AS created_at
            FROM chat_room_message m
            JOIN user u ON u.id = m.user_id
            WHERE m.id = #{id}
            """)
    Map<String, Object> findMessageById(Long id);

    @Select("""
            <script>
            SELECT m.id, m.user_id, m.room_type, m.room_id, u.username, u.avatar, m.content, m.create_time AS created_at
            FROM chat_room_message m
            JOIN user u ON u.id = m.user_id
            <where>
                m.room_type = #{roomType}
                <choose>
                    <when test="roomId != null">
                        AND m.room_id = #{roomId}
                    </when>
                    <otherwise>
                        AND m.room_id IS NULL
                    </otherwise>
                </choose>
                <if test="before != null and before != ''">
                    AND m.create_time &lt; #{before}
                </if>
            </where>
            ORDER BY m.create_time DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Map<String, Object>> findMessages(
            @Param("before") String before,
            @Param("roomType") String roomType,
            @Param("roomId") Long roomId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM chat_room_message
            <where>
                room_type = #{roomType}
                <choose>
                    <when test="roomId != null">
                        AND room_id = #{roomId}
                    </when>
                    <otherwise>
                        AND room_id IS NULL
                    </otherwise>
                </choose>
                <if test="before != null and before != ''">
                    AND create_time &lt; #{before}
                </if>
            </where>
            </script>
            """)
    int countMessages(@Param("before") String before, @Param("roomType") String roomType, @Param("roomId") Long roomId);
}
