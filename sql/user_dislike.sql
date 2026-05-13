/*
  用户「不感兴趣」负反馈（推荐过滤/标签降权）
  请在已存在 user、novel_book_main 表的数据库上执行（例如在 novel_db.sql 初始化之后）。
*/

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_dislike_novel` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户 id',
  `novel_id` int NOT NULL COMMENT '不再推荐的小说 id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dislike_novel` (`user_id`, `novel_id`),
  KEY `idx_dislike_novel_user` (`user_id`),
  CONSTRAINT `fk_dislike_novel_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_dislike_novel_book` FOREIGN KEY (`novel_id`) REFERENCES `novel_book_main` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户不感兴趣-单书屏蔽';

CREATE TABLE IF NOT EXISTS `user_dislike_author` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户 id',
  `author_name` varchar(100) NOT NULL COMMENT '不再推荐的作者名（与 novel_book_main.author trim 后一致）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dislike_author` (`user_id`, `author_name`),
  KEY `idx_dislike_author_user` (`user_id`),
  CONSTRAINT `fk_dislike_author_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户不感兴趣-作者屏蔽';

CREATE TABLE IF NOT EXISTS `user_dislike_tag` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户 id',
  `tag_name` varchar(50) NOT NULL COMMENT '推荐中降权的标签名',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dislike_tag` (`user_id`, `tag_name`),
  KEY `idx_dislike_tag_user` (`user_id`),
  CONSTRAINT `fk_dislike_tag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户不感兴趣-标签降权';
