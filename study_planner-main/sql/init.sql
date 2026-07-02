-- ============================================
-- Smart Study Planner database initialization
-- Fresh setup / local reset script.
-- This script includes core tables, forum tables, plan workbench tables,
-- default forum topics, and statistics views.
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `study_planner`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `study_planner`;

DROP VIEW IF EXISTS `v_user_study_stats`;
DROP VIEW IF EXISTS `v_plan_progress`;

DROP TABLE IF EXISTS `forum_topic_suggestion`;
DROP TABLE IF EXISTS `forum_topic_follow`;
DROP TABLE IF EXISTS `forum_user_follow`;
DROP TABLE IF EXISTS `forum_answer_collection`;
DROP TABLE IF EXISTS `forum_answer_vote`;
DROP TABLE IF EXISTS `forum_question_view`;
DROP TABLE IF EXISTS `forum_question_follow`;
DROP TABLE IF EXISTS `forum_question_favorite`;
DROP TABLE IF EXISTS `forum_question_vote`;
DROP TABLE IF EXISTS `forum_comment`;
DROP TABLE IF EXISTS `forum_answer`;
DROP TABLE IF EXISTS `forum_question_topic`;
DROP TABLE IF EXISTS `forum_question`;
DROP TABLE IF EXISTS `forum_topic`;
DROP TABLE IF EXISTS `plan_edit_version`;
DROP TABLE IF EXISTS `chat_room_message`;
DROP TABLE IF EXISTS `chat_history`;
DROP TABLE IF EXISTS `check_in`;
DROP TABLE IF EXISTS `plan_member`;
DROP TABLE IF EXISTS `plan_detail`;
DROP TABLE IF EXISTS `study_plan`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'user',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `study_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `goal` TEXT NOT NULL,
  `level` VARCHAR(20) DEFAULT '零基础',
  `daily_hours` DECIMAL(3,1) DEFAULT 2.0,
  `total_days` INT DEFAULT 30,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `status` VARCHAR(20) DEFAULT '进行中',
  `plan_type` VARCHAR(20) NOT NULL DEFAULT 'personal',
  `invite_code` VARCHAR(16) DEFAULT NULL,
  `source_group_plan_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_plan_invite_code` (`invite_code`),
  KEY `idx_study_plan_user` (`user_id`),
  KEY `idx_study_plan_status` (`status`),
  KEY `idx_study_plan_plan_type` (`plan_type`),
  KEY `idx_study_plan_source_group` (`source_group_plan_id`),
  CONSTRAINT `fk_study_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_study_plan_source_group` FOREIGN KEY (`source_group_plan_id`) REFERENCES `study_plan` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `plan_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL,
  `day_number` INT NOT NULL,
  `content` TEXT NOT NULL,
  `duration` DECIMAL(3,1) DEFAULT 2.0,
  `scheduled_date` DATE DEFAULT NULL,
  `sort_order` INT DEFAULT NULL,
  `is_completed` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_detail_plan` (`plan_id`),
  KEY `idx_plan_detail_day` (`day_number`),
  KEY `idx_plan_detail_schedule` (`scheduled_date`, `sort_order`),
  CONSTRAINT `fk_plan_detail_plan` FOREIGN KEY (`plan_id`) REFERENCES `study_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `plan_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'member',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_member` (`plan_id`, `user_id`),
  KEY `idx_plan_member_plan` (`plan_id`),
  KEY `idx_plan_member_user` (`user_id`),
  CONSTRAINT `fk_plan_member_plan` FOREIGN KEY (`plan_id`) REFERENCES `study_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `check_in` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `detail_id` BIGINT NOT NULL,
  `check_date` DATE NOT NULL,
  `study_hours` DECIMAL(3,1) DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_in_user_date_detail` (`user_id`, `check_date`, `detail_id`),
  KEY `idx_check_in_user` (`user_id`),
  KEY `idx_check_in_plan` (`plan_id`),
  KEY `idx_check_in_date` (`check_date`),
  KEY `idx_check_in_detail` (`detail_id`),
  CONSTRAINT `fk_check_in_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_check_in_plan` FOREIGN KEY (`plan_id`) REFERENCES `study_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_check_in_detail` FOREIGN KEY (`detail_id`) REFERENCES `plan_detail` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `question` TEXT NOT NULL,
  `answer` TEXT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_history_user` (`user_id`),
  CONSTRAINT `fk_chat_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `room_type` VARCHAR(20) NOT NULL DEFAULT 'global',
  `room_id` BIGINT DEFAULT NULL,
  `content` TEXT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_room_message_room` (`room_type`, `room_id`, `create_time`),
  KEY `idx_chat_room_message_user` (`user_id`),
  CONSTRAINT `fk_chat_room_message_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `plan_edit_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `snapshot` JSON NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_edit_version_plan` (`plan_id`),
  KEY `idx_plan_edit_version_user` (`user_id`),
  CONSTRAINT `fk_plan_edit_version_plan` FOREIGN KEY (`plan_id`) REFERENCES `study_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_edit_version_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT '',
  `follow_count` INT DEFAULT 0,
  `question_count` INT DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_topic_name` (`name`),
  KEY `idx_forum_topic_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT,
  `anonymous` TINYINT DEFAULT 0,
  `view_count` INT DEFAULT 0,
  `answer_count` INT DEFAULT 0,
  `vote_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `follow_count` INT DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_question_user` (`user_id`),
  KEY `idx_forum_question_status_time` (`status`, `create_time`),
  KEY `idx_forum_question_create_time` (`create_time`),
  CONSTRAINT `fk_forum_question_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_question_topic` (
  `question_id` BIGINT NOT NULL,
  `topic_id` BIGINT NOT NULL,
  PRIMARY KEY (`question_id`, `topic_id`),
  KEY `idx_forum_qt_topic` (`topic_id`),
  CONSTRAINT `fk_forum_qt_question` FOREIGN KEY (`question_id`) REFERENCES `forum_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_qt_topic` FOREIGN KEY (`topic_id`) REFERENCES `forum_topic` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `vote_count` INT DEFAULT 0,
  `collect_count` INT DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_answer_question` (`question_id`),
  KEY `idx_forum_answer_user` (`user_id`),
  KEY `idx_forum_answer_status_time` (`status`, `create_time`),
  CONSTRAINT `fk_forum_answer_question` FOREIGN KEY (`question_id`) REFERENCES `forum_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_answer_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `answer_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `parent_id` BIGINT DEFAULT NULL,
  `content` TEXT NOT NULL,
  `vote_count` INT DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_comment_answer` (`answer_id`),
  KEY `idx_forum_comment_user` (`user_id`),
  KEY `idx_forum_comment_parent` (`parent_id`),
  KEY `idx_forum_comment_status_time` (`status`, `create_time`),
  CONSTRAINT `fk_forum_comment_answer` FOREIGN KEY (`answer_id`) REFERENCES `forum_answer` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `forum_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_question_vote` (
  `user_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `question_id`),
  KEY `idx_forum_qv_question` (`question_id`),
  CONSTRAINT `fk_forum_qv_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_qv_question` FOREIGN KEY (`question_id`) REFERENCES `forum_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_question_favorite` (
  `user_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `question_id`),
  KEY `idx_forum_qf_question` (`question_id`),
  CONSTRAINT `fk_forum_qf_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_qf_question` FOREIGN KEY (`question_id`) REFERENCES `forum_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_question_follow` (
  `user_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `question_id`),
  KEY `idx_forum_qfollow_question` (`question_id`),
  CONSTRAINT `fk_forum_qfollow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_qfollow_question` FOREIGN KEY (`question_id`) REFERENCES `forum_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_question_view` (
  `question_id` BIGINT NOT NULL,
  `view_key` VARCHAR(128) NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `session_id` VARCHAR(128) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`question_id`, `view_key`),
  KEY `idx_forum_qview_user` (`user_id`),
  CONSTRAINT `fk_forum_qview_question` FOREIGN KEY (`question_id`) REFERENCES `forum_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_qview_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_answer_vote` (
  `user_id` BIGINT NOT NULL,
  `answer_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `answer_id`),
  KEY `idx_forum_av_answer` (`answer_id`),
  CONSTRAINT `fk_forum_av_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_av_answer` FOREIGN KEY (`answer_id`) REFERENCES `forum_answer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_answer_collection` (
  `user_id` BIGINT NOT NULL,
  `answer_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `answer_id`),
  KEY `idx_forum_ac_answer` (`answer_id`),
  CONSTRAINT `fk_forum_ac_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_ac_answer` FOREIGN KEY (`answer_id`) REFERENCES `forum_answer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_user_follow` (
  `follower_id` BIGINT NOT NULL,
  `following_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`follower_id`, `following_id`),
  KEY `idx_forum_uf_following` (`following_id`),
  CONSTRAINT `fk_forum_uf_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_uf_following` FOREIGN KEY (`following_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_topic_follow` (
  `user_id` BIGINT NOT NULL,
  `topic_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `topic_id`),
  KEY `idx_forum_tf_topic` (`topic_id`),
  CONSTRAINT `fk_forum_tf_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_tf_topic` FOREIGN KEY (`topic_id`) REFERENCES `forum_topic` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `forum_topic_suggestion` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT '',
  `reason` VARCHAR(255) DEFAULT '',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `reviewer_id` BIGINT DEFAULT NULL,
  `review_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_ts_user` (`user_id`),
  KEY `idx_forum_ts_status` (`status`),
  KEY `idx_forum_ts_reviewer` (`reviewer_id`),
  CONSTRAINT `fk_forum_ts_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_ts_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `forum_topic` (`name`, `description`) VALUES
('学习方法', '交流学习效率、时间管理和复盘方法'),
('编程入门', 'Python、Java、前端等编程学习问题'),
('考研备考', '考研计划、资料选择和进度管理'),
('英语学习', '单词、听力、阅读和写作训练'),
('AI工具', '用AI辅助学习和制定计划');

CREATE OR REPLACE VIEW `v_user_study_stats` AS
SELECT
  u.id AS user_id,
  u.username,
  COUNT(DISTINCT sp.id) AS total_plans,
  COUNT(DISTINCT ci.id) AS total_checkins,
  COALESCE(SUM(ci.study_hours), 0) AS total_study_hours
FROM `user` u
LEFT JOIN `study_plan` sp ON sp.user_id = u.id
LEFT JOIN `check_in` ci ON ci.user_id = u.id
GROUP BY u.id, u.username;

CREATE OR REPLACE VIEW `v_plan_progress` AS
SELECT
  sp.id AS plan_id,
  sp.title,
  sp.user_id,
  sp.total_days,
  COUNT(pd.id) AS total_tasks,
  COALESCE(SUM(pd.is_completed), 0) AS completed_tasks,
  CASE
    WHEN COUNT(pd.id) = 0 THEN 0
    ELSE ROUND(COALESCE(SUM(pd.is_completed), 0) / COUNT(pd.id) * 100, 2)
  END AS progress_percent
FROM `study_plan` sp
LEFT JOIN `plan_detail` pd ON pd.plan_id = sp.id
GROUP BY sp.id, sp.title, sp.user_id, sp.total_days;

SELECT 'study_planner database initialized' AS message;
