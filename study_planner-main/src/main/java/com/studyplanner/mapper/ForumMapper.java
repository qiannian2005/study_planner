package com.studyplanner.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ForumMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_topic (
                id BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(50) NOT NULL,
                description VARCHAR(255) DEFAULT '',
                follow_count INT DEFAULT 0,
                question_count INT DEFAULT 0,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_forum_topic_name (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createTopicTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_question (
                id BIGINT NOT NULL AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                title VARCHAR(200) NOT NULL,
                content TEXT,
                anonymous TINYINT DEFAULT 0,
                view_count INT DEFAULT 0,
                answer_count INT DEFAULT 0,
                vote_count INT DEFAULT 0,
                favorite_count INT DEFAULT 0,
                follow_count INT DEFAULT 0,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_forum_question_user (user_id),
                KEY idx_forum_question_create_time (create_time),
                CONSTRAINT fk_forum_question_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createQuestionTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_question_topic (
                question_id BIGINT NOT NULL,
                topic_id BIGINT NOT NULL,
                PRIMARY KEY (question_id, topic_id),
                KEY idx_forum_qt_topic (topic_id),
                CONSTRAINT fk_forum_qt_question FOREIGN KEY (question_id) REFERENCES forum_question(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_qt_topic FOREIGN KEY (topic_id) REFERENCES forum_topic(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createQuestionTopicTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_answer (
                id BIGINT NOT NULL AUTO_INCREMENT,
                question_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                content TEXT NOT NULL,
                vote_count INT DEFAULT 0,
                collect_count INT DEFAULT 0,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_forum_answer_question (question_id),
                CONSTRAINT fk_forum_answer_question FOREIGN KEY (question_id) REFERENCES forum_question(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_answer_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createAnswerTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_comment (
                id BIGINT NOT NULL AUTO_INCREMENT,
                answer_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                parent_id BIGINT DEFAULT NULL,
                content TEXT NOT NULL,
                vote_count INT DEFAULT 0,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_forum_comment_answer (answer_id),
                KEY idx_forum_comment_parent (parent_id),
                CONSTRAINT fk_forum_comment_answer FOREIGN KEY (answer_id) REFERENCES forum_answer(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_comment_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_comment_parent FOREIGN KEY (parent_id) REFERENCES forum_comment(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createCommentTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_question_vote (
                user_id BIGINT NOT NULL,
                question_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, question_id),
                KEY idx_forum_qv_question (question_id),
                CONSTRAINT fk_forum_qv_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_qv_question FOREIGN KEY (question_id) REFERENCES forum_question(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createQuestionVoteTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_question_favorite (
                user_id BIGINT NOT NULL,
                question_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, question_id),
                KEY idx_forum_qf_question (question_id),
                CONSTRAINT fk_forum_qf_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_qf_question FOREIGN KEY (question_id) REFERENCES forum_question(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createQuestionFavoriteTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_question_follow (
                user_id BIGINT NOT NULL,
                question_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, question_id),
                KEY idx_forum_qfollow_question (question_id),
                CONSTRAINT fk_forum_qfollow_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_qfollow_question FOREIGN KEY (question_id) REFERENCES forum_question(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createQuestionFollowTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_question_view (
                question_id BIGINT NOT NULL,
                view_key VARCHAR(128) NOT NULL,
                user_id BIGINT DEFAULT NULL,
                session_id VARCHAR(128) DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (question_id, view_key),
                KEY idx_forum_qview_user (user_id),
                CONSTRAINT fk_forum_qview_question FOREIGN KEY (question_id) REFERENCES forum_question(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_qview_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createQuestionViewTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_answer_vote (
                user_id BIGINT NOT NULL,
                answer_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, answer_id),
                KEY idx_forum_av_answer (answer_id),
                CONSTRAINT fk_forum_av_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_av_answer FOREIGN KEY (answer_id) REFERENCES forum_answer(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createAnswerVoteTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_answer_collection (
                user_id BIGINT NOT NULL,
                answer_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, answer_id),
                KEY idx_forum_ac_answer (answer_id),
                CONSTRAINT fk_forum_ac_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_ac_answer FOREIGN KEY (answer_id) REFERENCES forum_answer(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createAnswerCollectionTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_user_follow (
                follower_id BIGINT NOT NULL,
                following_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (follower_id, following_id),
                KEY idx_forum_uf_following (following_id),
                CONSTRAINT fk_forum_uf_follower FOREIGN KEY (follower_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_uf_following FOREIGN KEY (following_id) REFERENCES user(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createUserFollowTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_topic_follow (
                user_id BIGINT NOT NULL,
                topic_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, topic_id),
                KEY idx_forum_tf_topic (topic_id),
                CONSTRAINT fk_forum_tf_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_tf_topic FOREIGN KEY (topic_id) REFERENCES forum_topic(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createTopicFollowTable();

    @Update("""
            CREATE TABLE IF NOT EXISTS forum_topic_suggestion (
                id BIGINT NOT NULL AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                name VARCHAR(50) NOT NULL,
                description VARCHAR(255) DEFAULT '',
                reason VARCHAR(255) DEFAULT '',
                status VARCHAR(20) NOT NULL DEFAULT 'pending',
                reviewer_id BIGINT DEFAULT NULL,
                review_time DATETIME DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_forum_ts_user (user_id),
                KEY idx_forum_ts_status (status),
                CONSTRAINT fk_forum_ts_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                CONSTRAINT fk_forum_ts_reviewer FOREIGN KEY (reviewer_id) REFERENCES user(id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """)
    void createTopicSuggestionTable();

    @Update("ALTER TABLE forum_topic ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER question_count")
    void addTopicStatusColumn();

    @Update("ALTER TABLE forum_question ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER follow_count")
    void addQuestionStatusColumn();

    @Update("ALTER TABLE forum_answer ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER collect_count")
    void addAnswerStatusColumn();

    @Update("ALTER TABLE forum_comment ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER vote_count")
    void addCommentStatusColumn();

    @Select("SELECT COUNT(*) FROM forum_topic")
    int countTopics();

    @Insert("""
            INSERT INTO forum_topic (name, description, follow_count, question_count)
            VALUES (#{name}, #{description}, 0, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTopic(Map<String, Object> topic);

    @Select("""
            SELECT id, name, description, follow_count, question_count, status, false AS is_followed
            FROM forum_topic
            WHERE status = 'active'
            ORDER BY question_count DESC, id ASC
            """)
    List<Map<String, Object>> findTopics();

    @Select("""
            SELECT id, name, description, follow_count, question_count, status, false AS is_followed
            FROM forum_topic
            WHERE id = #{id} AND status = 'active'
            """)
    Map<String, Object> findTopicById(Long id);

    @Select("""
            SELECT id, name, description, follow_count, question_count, status, false AS is_followed
            FROM forum_topic
            WHERE name = #{name} AND status = 'active'
            """)
    Map<String, Object> findTopicByName(String name);

    @Select("""
            SELECT id, name, description, follow_count, question_count, status, false AS is_followed
            FROM forum_topic
            WHERE name = #{name}
            """)
    Map<String, Object> findAnyTopicByName(String name);

    @Select("""
            SELECT COUNT(*) FROM forum_topic_follow
            WHERE user_id = #{userId} AND topic_id = #{topicId}
            """)
    int countTopicFollow(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Insert("""
            INSERT IGNORE INTO forum_topic_follow (user_id, topic_id)
            VALUES (#{userId}, #{topicId})
            """)
    int insertTopicFollow(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Delete("""
            DELETE FROM forum_topic_follow
            WHERE user_id = #{userId} AND topic_id = #{topicId}
            """)
    int deleteTopicFollow(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Update("""
            UPDATE forum_topic
            SET follow_count = (SELECT COUNT(*) FROM forum_topic_follow WHERE topic_id = #{topicId})
            WHERE id = #{topicId}
            """)
    int refreshTopicFollowCount(Long topicId);

    @Select("SELECT follow_count FROM forum_topic WHERE id = #{topicId}")
    Integer findTopicFollowCount(Long topicId);

    @Select("""
            SELECT t.id, t.name, t.description, t.follow_count, t.question_count, true AS is_followed
            FROM forum_topic_follow tf
            JOIN forum_topic t ON t.id = tf.topic_id
            WHERE tf.user_id = #{userId} AND t.status = 'active'
            ORDER BY tf.create_time DESC
            """)
    List<Map<String, Object>> findFollowedTopicsByUser(Long userId);

    @Insert("""
            INSERT INTO forum_question (user_id, title, content, anonymous)
            VALUES (#{userId}, #{title}, #{content}, #{anonymous})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQuestion(Map<String, Object> question);

    @Insert("""
            INSERT IGNORE INTO forum_question_topic (question_id, topic_id)
            VALUES (#{questionId}, #{topicId})
            """)
    int insertQuestionTopic(@Param("questionId") Long questionId, @Param("topicId") Long topicId);

    @Update("""
            UPDATE forum_topic t
            SET question_count = (
                SELECT COUNT(*) FROM forum_question_topic qt WHERE qt.topic_id = t.id
            )
            WHERE t.id = #{topicId}
            """)
    int refreshTopicQuestionCount(Long topicId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            WHERE q.status = 'active'
            ORDER BY q.create_time DESC
            """)
    List<Map<String, Object>> findQuestions();

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            WHERE q.status = 'active'
            ORDER BY q.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> findQuestionsPaged(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM forum_question q WHERE q.status = 'active'")
    long countQuestions();

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            WHERE q.id = #{id} AND q.status = 'active'
            """)
    Map<String, Object> findQuestionById(Long id);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            JOIN forum_question_topic qt ON qt.question_id = q.id
            WHERE qt.topic_id = #{topicId} AND q.status = 'active'
            ORDER BY q.create_time DESC
            """)
    List<Map<String, Object>> findQuestionsByTopic(Long topicId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            JOIN forum_question_topic qt ON qt.question_id = q.id
            WHERE qt.topic_id = #{topicId} AND q.status = 'active'
            ORDER BY q.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> findQuestionsByTopicPaged(
            @Param("topicId") Long topicId, @Param("offset") int offset, @Param("size") int size);

    @Select("""
            SELECT COUNT(*) FROM forum_question q
            JOIN forum_question_topic qt ON qt.question_id = q.id
            WHERE qt.topic_id = #{topicId} AND q.status = 'active'
            """)
    long countQuestionsByTopic(@Param("topicId") Long topicId);

    @Select("""
            SELECT u.id, u.username, u.avatar,
                   COUNT(a.id) AS answer_count,
                   COALESCE(SUM(a.vote_count), 0) AS vote_count,
                   MAX(a.create_time) AS last_answer_at
            FROM forum_answer a
            JOIN forum_question q ON q.id = a.question_id
            JOIN forum_question_topic qt ON qt.question_id = q.id
            JOIN user u ON u.id = a.user_id
            WHERE qt.topic_id = #{topicId}
              AND q.status = 'active'
              AND a.status = 'active'
            GROUP BY u.id, u.username, u.avatar
            ORDER BY answer_count DESC, vote_count DESC, last_answer_at DESC
            LIMIT 5
            """)
    List<Map<String, Object>> findTopAnswerersByTopic(Long topicId);

    @Select("""
            SELECT a.id, a.question_id, a.content, a.vote_count, a.collect_count,
                   a.create_time AS created_at, a.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar,
                   q.title AS question_title,
                   (SELECT COUNT(*) FROM forum_comment c WHERE c.answer_id = a.id AND c.status = 'active') AS comment_count
            FROM forum_answer a
            JOIN forum_question q ON q.id = a.question_id
            JOIN forum_question_topic qt ON qt.question_id = q.id
            JOIN user u ON u.id = a.user_id
            WHERE qt.topic_id = #{topicId}
              AND q.status = 'active'
              AND a.status = 'active'
            ORDER BY a.vote_count DESC, comment_count DESC, a.create_time DESC
            LIMIT 5
            """)
    List<Map<String, Object>> findTopAnswersByTopic(Long topicId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            WHERE q.user_id = #{userId} AND q.status = 'active'
            ORDER BY q.create_time DESC
            """)
    List<Map<String, Object>> findQuestionsByUser(Long userId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            WHERE q.user_id = #{userId} AND q.status = 'active'
            ORDER BY q.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> findQuestionsByUserPaged(
            @Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM forum_question q WHERE q.user_id = #{userId} AND q.status = 'active'")
    long countQuestionsByUser(@Param("userId") Long userId);

    @Select("""
            SELECT t.id, t.name, t.description, t.follow_count, t.question_count, false AS is_followed
            FROM forum_topic t
            JOIN forum_question_topic qt ON qt.topic_id = t.id
            WHERE qt.question_id = #{questionId} AND t.status = 'active'
            ORDER BY t.id ASC
            """)
    List<Map<String, Object>> findTopicsByQuestion(Long questionId);

    @Select("""
            SELECT COUNT(*) FROM forum_question_follow
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int countQuestionFollow(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Insert("""
            INSERT IGNORE INTO forum_question_follow (user_id, question_id)
            VALUES (#{userId}, #{questionId})
            """)
    int insertQuestionFollow(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Delete("""
            DELETE FROM forum_question_follow
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int deleteQuestionFollow(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Update("""
            UPDATE forum_question
            SET follow_count = (SELECT COUNT(*) FROM forum_question_follow WHERE question_id = #{questionId})
            WHERE id = #{questionId}
            """)
    int refreshQuestionFollowCount(Long questionId);

    @Update("""
            UPDATE forum_question q
            SET follow_count = (SELECT COUNT(*) FROM forum_question_follow f WHERE f.question_id = q.id)
            """)
    int refreshAllQuestionFollowCounts();

    @Select("SELECT follow_count FROM forum_question WHERE id = #{questionId}")
    Integer findQuestionFollowCount(Long questionId);

    @Insert("""
            INSERT IGNORE INTO forum_question_view (question_id, view_key, user_id, session_id)
            VALUES (#{questionId}, #{viewKey}, #{userId}, #{sessionId})
            """)
    int insertQuestionView(
            @Param("questionId") Long questionId,
            @Param("viewKey") String viewKey,
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId
    );

    @Update("""
            UPDATE forum_question
            SET view_count = (SELECT COUNT(*) FROM forum_question_view WHERE question_id = #{questionId})
            WHERE id = #{questionId}
            """)
    int refreshQuestionViewCount(Long questionId);

    @Update("""
            UPDATE forum_question q
            SET view_count = (SELECT COUNT(*) FROM forum_question_view v WHERE v.question_id = q.id)
            """)
    int refreshAllQuestionViewCounts();

    @Select("""
            SELECT COUNT(*) FROM forum_question_vote
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int countQuestionVote(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Insert("""
            INSERT IGNORE INTO forum_question_vote (user_id, question_id)
            VALUES (#{userId}, #{questionId})
            """)
    int insertQuestionVote(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Delete("""
            DELETE FROM forum_question_vote
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int deleteQuestionVote(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Update("""
            UPDATE forum_question
            SET vote_count = (SELECT COUNT(*) FROM forum_question_vote WHERE question_id = #{questionId})
            WHERE id = #{questionId}
            """)
    int refreshQuestionVoteCount(Long questionId);

    @Select("SELECT vote_count FROM forum_question WHERE id = #{questionId}")
    Integer findQuestionVoteCount(Long questionId);

    @Select("""
            SELECT COUNT(*) FROM forum_question_favorite
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int countQuestionFavorite(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Insert("""
            INSERT IGNORE INTO forum_question_favorite (user_id, question_id)
            VALUES (#{userId}, #{questionId})
            """)
    int insertQuestionFavorite(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Delete("""
            DELETE FROM forum_question_favorite
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int deleteQuestionFavorite(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Update("""
            UPDATE forum_question
            SET favorite_count = (SELECT COUNT(*) FROM forum_question_favorite WHERE question_id = #{questionId})
            WHERE id = #{questionId}
            """)
    int refreshQuestionFavoriteCount(Long questionId);

    @Select("SELECT favorite_count FROM forum_question WHERE id = #{questionId}")
    Integer findQuestionFavoriteCount(Long questionId);

    @Select("""
            SELECT qf.question_id AS id, qf.question_id AS target_id, qf.create_time AS created_at,
                   q.title, q.title AS target_title, q.content, q.view_count, q.answer_count,
                   q.vote_count, q.favorite_count, q.follow_count,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question_favorite qf
            JOIN forum_question q ON q.id = qf.question_id
            JOIN user u ON u.id = q.user_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            ORDER BY qf.create_time DESC
            """)
    List<Map<String, Object>> findQuestionFavoritesByUser(Long userId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question_favorite qf
            JOIN forum_question q ON q.id = qf.question_id
            JOIN user u ON u.id = q.user_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            ORDER BY qf.create_time DESC
            """)
    List<Map<String, Object>> findFavoriteQuestionsByUser(Long userId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question_favorite qf
            JOIN forum_question q ON q.id = qf.question_id
            JOIN user u ON u.id = q.user_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            ORDER BY qf.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> findFavoriteQuestionsByUserPaged(
            @Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("""
            SELECT COUNT(*) FROM forum_question_favorite qf
            JOIN forum_question q ON q.id = qf.question_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            """)
    long countFavoriteQuestionsByUser(@Param("userId") Long userId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question_follow qf
            JOIN forum_question q ON q.id = qf.question_id
            JOIN user u ON u.id = q.user_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            ORDER BY qf.create_time DESC
            """)
    List<Map<String, Object>> findFollowedQuestionsByUser(Long userId);

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question_follow qf
            JOIN forum_question q ON q.id = qf.question_id
            JOIN user u ON u.id = q.user_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            ORDER BY qf.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> findFollowedQuestionsByUserPaged(
            @Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("""
            SELECT COUNT(*) FROM forum_question_follow qf
            JOIN forum_question q ON q.id = qf.question_id
            WHERE qf.user_id = #{userId} AND q.status = 'active'
            """)
    long countFollowedQuestionsByUser(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO forum_answer (question_id, user_id, content)
            VALUES (#{questionId}, #{userId}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAnswer(Map<String, Object> answer);

    @Update("""
            UPDATE forum_question
            SET answer_count = (SELECT COUNT(*) FROM forum_answer WHERE question_id = #{questionId})
            WHERE id = #{questionId}
            """)
    int refreshQuestionAnswerCount(Long questionId);

    @Select("""
            SELECT a.id, a.question_id, a.content, a.vote_count, a.collect_count,
                   a.create_time AS created_at, a.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar,
                   (SELECT COUNT(*) FROM forum_comment c WHERE c.answer_id = a.id) AS comment_count
            FROM forum_answer a
            JOIN user u ON u.id = a.user_id
            WHERE a.question_id = #{questionId} AND a.status = 'active'
            ORDER BY a.create_time ASC
            """)
    List<Map<String, Object>> findAnswersByQuestion(Long questionId);

    @Select("""
            SELECT a.id, a.question_id, a.content, a.vote_count, a.collect_count,
                   a.create_time AS created_at, a.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar,
                   (SELECT COUNT(*) FROM forum_comment c WHERE c.answer_id = a.id) AS comment_count
            FROM forum_answer a
            JOIN user u ON u.id = a.user_id
            WHERE a.question_id = #{questionId} AND a.status = 'active'
            ORDER BY a.create_time DESC
            """)
    List<Map<String, Object>> findAnswersByQuestionOrderByTime(Long questionId);

    @Select("""
            SELECT a.id, a.question_id, a.content, a.vote_count, a.collect_count,
                   a.create_time AS created_at, a.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar,
                   (SELECT COUNT(*) FROM forum_comment c WHERE c.answer_id = a.id) AS comment_count
            FROM forum_answer a
            JOIN user u ON u.id = a.user_id
            WHERE a.question_id = #{questionId} AND a.status = 'active'
            ORDER BY a.vote_count DESC, a.create_time DESC
            """)
    List<Map<String, Object>> findAnswersByQuestionOrderByVote(Long questionId);

    @Select("""
            SELECT a.id, a.question_id, a.content, a.vote_count, a.collect_count,
                   a.create_time AS created_at, a.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar,
                   q.title AS question_title
            FROM forum_answer a
            JOIN user u ON u.id = a.user_id
            JOIN forum_question q ON q.id = a.question_id
            WHERE a.user_id = #{userId} AND a.status = 'active' AND q.status = 'active'
            ORDER BY a.create_time DESC
            """)
    List<Map<String, Object>> findAnswersByUser(Long userId);

    @Select("""
            SELECT ac.answer_id AS id, q.id AS target_id, ac.create_time AS created_at,
                   q.title AS target_title, a.content AS answer_content
            FROM forum_answer_collection ac
            JOIN forum_answer a ON a.id = ac.answer_id
            JOIN forum_question q ON q.id = a.question_id
            WHERE ac.user_id = #{userId} AND a.status = 'active' AND q.status = 'active'
            ORDER BY ac.create_time DESC
            """)
    List<Map<String, Object>> findAnswerCollectionsByUser(Long userId);

    @Select("""
            SELECT COUNT(*) FROM forum_answer_vote
            WHERE user_id = #{userId} AND answer_id = #{answerId}
            """)
    int countAnswerVote(@Param("userId") Long userId, @Param("answerId") Long answerId);

    @Insert("""
            INSERT IGNORE INTO forum_answer_vote (user_id, answer_id)
            VALUES (#{userId}, #{answerId})
            """)
    int insertAnswerVote(@Param("userId") Long userId, @Param("answerId") Long answerId);

    @Delete("""
            DELETE FROM forum_answer_vote
            WHERE user_id = #{userId} AND answer_id = #{answerId}
            """)
    int deleteAnswerVote(@Param("userId") Long userId, @Param("answerId") Long answerId);

    @Update("""
            UPDATE forum_answer
            SET vote_count = (SELECT COUNT(*) FROM forum_answer_vote WHERE answer_id = #{answerId})
            WHERE id = #{answerId}
            """)
    int refreshAnswerVoteCount(Long answerId);

    @Select("SELECT vote_count FROM forum_answer WHERE id = #{answerId}")
    Integer findAnswerVoteCount(Long answerId);

    @Select("""
            SELECT COUNT(*) FROM forum_answer_collection
            WHERE user_id = #{userId} AND answer_id = #{answerId}
            """)
    int countAnswerCollection(@Param("userId") Long userId, @Param("answerId") Long answerId);

    @Insert("""
            INSERT IGNORE INTO forum_answer_collection (user_id, answer_id)
            VALUES (#{userId}, #{answerId})
            """)
    int insertAnswerCollection(@Param("userId") Long userId, @Param("answerId") Long answerId);

    @Delete("""
            DELETE FROM forum_answer_collection
            WHERE user_id = #{userId} AND answer_id = #{answerId}
            """)
    int deleteAnswerCollection(@Param("userId") Long userId, @Param("answerId") Long answerId);

    @Update("""
            UPDATE forum_answer
            SET collect_count = (SELECT COUNT(*) FROM forum_answer_collection WHERE answer_id = #{answerId})
            WHERE id = #{answerId}
            """)
    int refreshAnswerCollectionCount(Long answerId);

    @Select("SELECT collect_count FROM forum_answer WHERE id = #{answerId}")
    Integer findAnswerCollectionCount(Long answerId);

    @Select("""
            SELECT u.id, u.username, u.avatar, u.create_time AS created_at,
                   (SELECT COUNT(*) FROM forum_question q WHERE q.user_id = u.id AND q.status = 'active') AS question_count,
                   (SELECT COUNT(*) FROM forum_answer a WHERE a.user_id = u.id AND a.status = 'active') AS answer_count,
                   (SELECT COALESCE(SUM(a.vote_count), 0) FROM forum_answer a WHERE a.user_id = u.id AND a.status = 'active') AS vote_count,
                   (
                       SELECT COUNT(*) FROM forum_question_favorite qf WHERE qf.user_id = u.id
                   ) + (
                       SELECT COUNT(*) FROM forum_answer_collection ac WHERE ac.user_id = u.id
                   ) AS favorite_count,
                   (SELECT COUNT(*) FROM forum_user_follow f WHERE f.follower_id = u.id) AS following_count,
                   (SELECT COUNT(*) FROM forum_user_follow f WHERE f.following_id = u.id) AS follower_count
            FROM user u
            WHERE u.id = #{userId}
            """)
    Map<String, Object> findForumUserById(Long userId);

    @Select("""
            SELECT COUNT(*) FROM forum_user_follow
            WHERE follower_id = #{followerId} AND following_id = #{followingId}
            """)
    int countUserFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Insert("""
            INSERT IGNORE INTO forum_user_follow (follower_id, following_id)
            VALUES (#{followerId}, #{followingId})
            """)
    int insertUserFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Delete("""
            DELETE FROM forum_user_follow
            WHERE follower_id = #{followerId} AND following_id = #{followingId}
            """)
    int deleteUserFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Select("""
            SELECT u.id, u.username, u.avatar, u.create_time AS created_at
            FROM forum_user_follow f
            JOIN user u ON u.id = f.following_id
            WHERE f.follower_id = #{userId}
            ORDER BY f.create_time DESC
            """)
    List<Map<String, Object>> findFollowingUsers(Long userId);

    @Select("""
            SELECT u.id, u.username, u.avatar, u.create_time AS created_at
            FROM forum_user_follow f
            JOIN user u ON u.id = f.follower_id
            WHERE f.following_id = #{userId}
            ORDER BY f.create_time DESC
            """)
    List<Map<String, Object>> findFollowerUsers(Long userId);

    @Insert("""
            INSERT INTO forum_comment (answer_id, user_id, parent_id, content)
            VALUES (#{answerId}, #{userId}, #{parentId}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertComment(Map<String, Object> comment);

    @Select("""
            SELECT c.id, c.answer_id, c.parent_id, c.content, c.vote_count,
                   c.create_time AS created_at, c.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar
            FROM forum_comment c
            JOIN user u ON u.id = c.user_id
            WHERE c.answer_id = #{answerId} AND c.parent_id IS NULL AND c.status = 'active'
            ORDER BY c.create_time ASC
            """)
    List<Map<String, Object>> findRootCommentsByAnswer(Long answerId);

    @Select("""
            SELECT c.id, c.answer_id, c.parent_id, c.content, c.vote_count,
                   c.create_time AS created_at, c.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar
            FROM forum_comment c
            JOIN user u ON u.id = c.user_id
            WHERE c.parent_id = #{parentId} AND c.status = 'active'
            ORDER BY c.create_time ASC
            """)
    List<Map<String, Object>> findRepliesByComment(Long parentId);

    @Select("""
            SELECT c.id, c.answer_id, c.parent_id, c.content, c.vote_count,
                   c.create_time AS created_at, c.user_id AS author_id,
                   u.username AS author_username, u.avatar AS author_avatar
            FROM forum_comment c
            JOIN user u ON u.id = c.user_id
            WHERE c.answer_id = #{answerId} AND c.status = 'active'
            ORDER BY c.create_time ASC
            """)
    List<Map<String, Object>> findCommentsByAnswer(Long answerId);

    @Update("UPDATE forum_comment SET vote_count = vote_count + 1 WHERE id = #{id}")
    int increaseCommentVote(Long id);

    @Select("SELECT vote_count FROM forum_comment WHERE id = #{id}")
    Integer findCommentVoteCount(Long id);

    @Select("""
            SELECT id, name, description, follow_count, question_count, status,
                   create_time AS created_at
            FROM forum_topic
            ORDER BY create_time DESC
            """)
    List<Map<String, Object>> findAdminTopics();

    @Select("""
            SELECT id, name, description, follow_count, question_count, status,
                   create_time AS created_at
            FROM forum_topic
            WHERE id = #{id}
            """)
    Map<String, Object> findAdminTopicById(Long id);

    @Update("""
            UPDATE forum_topic
            SET name = #{name}, description = #{description}
            WHERE id = #{id}
            """)
    int updateTopic(Map<String, Object> topic);

    @Update("UPDATE forum_topic SET status = #{status} WHERE id = #{id}")
    int updateTopicStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM forum_topic WHERE id = #{id}")
    int deleteTopicById(Long id);

    @Insert("""
            INSERT INTO forum_topic_suggestion (user_id, name, description, reason, status)
            VALUES (#{userId}, #{name}, #{description}, #{reason}, 'pending')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTopicSuggestion(Map<String, Object> suggestion);

    @Select("""
            SELECT ts.id, ts.user_id, u.username AS suggester_username, ts.name, ts.description,
                   ts.reason, ts.status, ts.reviewer_id, reviewer.username AS reviewer_username,
                   ts.review_time, ts.create_time AS created_at, ts.update_time AS updated_at
            FROM forum_topic_suggestion ts
            JOIN user u ON u.id = ts.user_id
            LEFT JOIN user reviewer ON reviewer.id = ts.reviewer_id
            ORDER BY
                CASE ts.status WHEN 'pending' THEN 0 WHEN 'approved' THEN 1 ELSE 2 END,
                ts.create_time DESC
            """)
    List<Map<String, Object>> findAdminTopicSuggestions();

    @Select("""
            SELECT ts.id, ts.user_id, u.username AS suggester_username, ts.name, ts.description,
                   ts.reason, ts.status, ts.reviewer_id, reviewer.username AS reviewer_username,
                   ts.review_time, ts.create_time AS created_at, ts.update_time AS updated_at
            FROM forum_topic_suggestion ts
            JOIN user u ON u.id = ts.user_id
            LEFT JOIN user reviewer ON reviewer.id = ts.reviewer_id
            WHERE ts.id = #{id}
            """)
    Map<String, Object> findTopicSuggestionById(Long id);

    @Update("""
            UPDATE forum_topic_suggestion
            SET status = #{status}, reviewer_id = #{reviewerId}, review_time = NOW()
            WHERE id = #{id}
            """)
    int updateTopicSuggestionStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("reviewerId") Long reviewerId
    );

    @Delete("DELETE FROM forum_topic_suggestion WHERE id = #{id}")
    int deleteTopicSuggestionById(Long id);

    @Select("""
            SELECT q.id, q.title, q.content, q.status, q.view_count, q.answer_count,
                   q.vote_count, q.favorite_count, q.follow_count, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            ORDER BY q.create_time DESC
            """)
    List<Map<String, Object>> findAdminQuestions();

    @Update("UPDATE forum_question SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateQuestionStatus(@Param("id") Long id, @Param("status") String status);

    @Select("""
            SELECT a.id, a.question_id, q.title AS question_title, a.content, a.status,
                   a.vote_count, a.collect_count, a.create_time AS created_at,
                   a.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_answer a
            JOIN user u ON u.id = a.user_id
            JOIN forum_question q ON q.id = a.question_id
            ORDER BY a.create_time DESC
            """)
    List<Map<String, Object>> findAdminAnswers();

    @Update("UPDATE forum_answer SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateAnswerStatus(@Param("id") Long id, @Param("status") String status);

    @Select("""
            SELECT c.id, c.answer_id, a.question_id, q.title AS question_title,
                   c.parent_id, c.content, c.status, c.vote_count, c.create_time AS created_at,
                   c.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_comment c
            JOIN user u ON u.id = c.user_id
            JOIN forum_answer a ON a.id = c.answer_id
            JOIN forum_question q ON q.id = a.question_id
            ORDER BY c.create_time DESC
            """)
    List<Map<String, Object>> findAdminComments();

    @Update("UPDATE forum_comment SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateCommentStatus(@Param("id") Long id, @Param("status") String status);

    // ========== 搜索相关 ==========

    @Select("""
            SELECT q.id, q.title, q.content, q.view_count, q.answer_count, q.vote_count,
                   q.favorite_count, q.follow_count, q.status, q.create_time AS created_at,
                   q.user_id AS author_id, u.username AS author_username, u.avatar AS author_avatar
            FROM forum_question q
            JOIN user u ON u.id = q.user_id
            WHERE q.status = 'active' AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY
              CASE WHEN #{sort} = 'time' THEN q.create_time END DESC,
              CASE WHEN #{sort} <> 'time' AND q.title = #{keyword} THEN 0 ELSE 1 END,
              CASE WHEN #{sort} <> 'time' AND q.title LIKE CONCAT(#{keyword}, '%') THEN 0 ELSE 1 END,
              CASE WHEN #{sort} <> 'time' THEN q.vote_count END DESC,
              CASE WHEN #{sort} <> 'time' THEN q.answer_count END DESC,
              CASE WHEN #{sort} <> 'time' THEN q.view_count END DESC,
              q.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> searchQuestions(
            @Param("keyword") String keyword, @Param("sort") String sort,
            @Param("offset") int offset, @Param("size") int size);

    @Select("""
            SELECT COUNT(*) FROM forum_question q
            WHERE q.status = 'active' AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countSearchQuestions(@Param("keyword") String keyword);

    @Select("""
            SELECT u.id, u.username, u.avatar, u.role, u.status, u.create_time AS created_at
            FROM user u
            WHERE u.username LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY
              CASE WHEN #{sort} = 'time' THEN u.create_time END DESC,
              CASE WHEN #{sort} <> 'time' AND u.username = #{keyword} THEN 0 ELSE 1 END,
              CASE WHEN #{sort} <> 'time' AND u.username LIKE CONCAT(#{keyword}, '%') THEN 0 ELSE 1 END,
              u.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> searchUsers(
            @Param("keyword") String keyword, @Param("sort") String sort,
            @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM user u WHERE u.username LIKE CONCAT('%', #{keyword}, '%')")
    long countSearchUsers(@Param("keyword") String keyword);

    @Select("""
            SELECT t.id, t.name, t.description, t.follow_count, t.question_count, t.create_time AS created_at, false AS is_followed
            FROM forum_topic t
            WHERE t.status = 'active' AND (t.name LIKE CONCAT('%', #{keyword}, '%') OR t.description LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY
              CASE WHEN #{sort} = 'time' THEN t.create_time END DESC,
              CASE WHEN #{sort} <> 'time' AND t.name = #{keyword} THEN 0 ELSE 1 END,
              CASE WHEN #{sort} <> 'time' AND t.name LIKE CONCAT(#{keyword}, '%') THEN 0 ELSE 1 END,
              CASE WHEN #{sort} <> 'time' THEN t.question_count END DESC,
              CASE WHEN #{sort} <> 'time' THEN t.follow_count END DESC,
              t.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> searchTopics(
            @Param("keyword") String keyword, @Param("sort") String sort,
            @Param("offset") int offset, @Param("size") int size);

    @Select("""
            SELECT COUNT(*) FROM forum_topic t
            WHERE t.status = 'active' AND (t.name LIKE CONCAT('%', #{keyword}, '%') OR t.description LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countSearchTopics(@Param("keyword") String keyword);

    @Select("""
            SELECT COUNT(*) FROM forum_question
            WHERE user_id = #{userId} AND status = 'active'
            """)
    long countTotalQuestionsByUser(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*) FROM forum_answer
            WHERE user_id = #{userId} AND status = 'active'
            """)
    long countTotalAnswersByUser(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(
                (SELECT COALESCE(SUM(q.vote_count), 0) FROM forum_question q WHERE q.user_id = #{userId} AND q.status = 'active')
                +
                (SELECT COALESCE(SUM(a.vote_count), 0) FROM forum_answer a WHERE a.user_id = #{userId} AND a.status = 'active')
            , 0)
            """)
    long countTotalVotesReceivedByUser(@Param("userId") Long userId);
}
