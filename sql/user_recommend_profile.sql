/*
  per-user 推荐配置（协同过滤开关 + 行为权重 + 阈值参数）
  MySQL 8 / utf8mb4
*/

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_recommend_profile` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '外键 user.id',

  `cf_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用物品协同(Item-CF)混合：0 否 1 是',
  `explain_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否返回/展示推荐解释：0 否 1 是',
  `mmr_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否对结果做MMR多样性重排：0 否 1 是',

  `w_collection` decimal(10,6) NOT NULL DEFAULT 1.000000 COMMENT '收藏行为权重（强正反馈）',
  `w_finished` decimal(10,6) NOT NULL DEFAULT 1.000000 COMMENT '完读行为权重（强正反馈）',
  `w_read_progress` decimal(10,6) NOT NULL DEFAULT 0.600000 COMMENT '阅读进度权重（0~1 归一化）',
  `w_read_duration` decimal(10,6) NOT NULL DEFAULT 0.400000 COMMENT '阅读时长权重（log 归一化）',
  `w_comment` decimal(10,6) NOT NULL DEFAULT 0.700000 COMMENT '评论行为权重（强度反馈）',

  `cold_start_collection_threshold` int NOT NULL DEFAULT 3 COMMENT '收藏少于该值时启用冷启动（标签画像/热度）',
  `recent_read_limit` int NOT NULL DEFAULT 30 COMMENT '读取最近阅读记录条数上限（用于画像与种子补足）',
  `topk_similar_per_seed` int NOT NULL DEFAULT 50 COMMENT '每个种子物品取相似TopK候选',
  `candidate_limit` int NOT NULL DEFAULT 200 COMMENT '候选集上限（召回后重排前）',
  `recommend_limit` int NOT NULL DEFAULT 20 COMMENT '最终推荐条数',

  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recommend_profile_user` (`user_id`),
  CONSTRAINT `fk_user_recommend_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户推荐配置（每用户一套）';

