-- ============================================================
-- 行为交互功能相关表结构（与后端 Mapper 一致）
-- 若 novel_db 中已有表但字段不一致，可参考本文件修改或重建
-- ============================================================

-- 用户阅读历史与时长表
-- 用于：阅读进度、阅读时长统计、最后阅读章节与时间
CREATE TABLE IF NOT EXISTS `user_reading_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `novel_id` int NOT NULL COMMENT '小说ID',
  `chapter_id` int NOT NULL COMMENT '章节ID',
  `read_duration` int NOT NULL DEFAULT 0 COMMENT '阅读时长(秒)，更新时累加',
  `read_progress` int DEFAULT NULL COMMENT '阅读进度(如当前页)',
  `last_read_time` datetime DEFAULT NULL COMMENT '最后阅读时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_novel` (`user_id`,`novel_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_novel_id` (`novel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户阅读历史和时长';

-- 用户完读记录表
-- 用于：记录用户已完读的小说，与收藏配合展示「已看完」
CREATE TABLE IF NOT EXISTS `user_finished_novel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `novel_id` int NOT NULL COMMENT '小说ID',
  `finished_time` datetime DEFAULT NULL COMMENT '完读时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_novel` (`user_id`,`novel_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_novel_id` (`novel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户完读记录表';

-- 用户评论表
-- 用于：小说详情页的读者评论与展示
CREATE TABLE IF NOT EXISTS `user_comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `novel_id` int NOT NULL COMMENT '小说ID',
  `content` varchar(1000) NOT NULL COMMENT '评论内容',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_novel_id` (`novel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户评论记录表';

-- 若表中缺少字段，可单独执行（按需替换表名与字段）：
-- ALTER TABLE user_reading_history ADD COLUMN read_duration int NOT NULL DEFAULT 0 COMMENT '阅读时长(秒)' AFTER chapter_id;
-- ALTER TABLE user_reading_history ADD COLUMN read_progress int DEFAULT NULL COMMENT '阅读进度' AFTER read_duration;
