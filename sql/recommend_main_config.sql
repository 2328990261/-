/*
  主推荐接口 /api/recommend/books 的全局超参（单行 id=1）
  MySQL 8 / utf8mb4
*/
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `recommend_main_config` (
  `id` int NOT NULL DEFAULT 1 COMMENT '固定为1',
  `pop_weight` decimal(10,6) NOT NULL DEFAULT 0.030000 COMMENT '热度系数：popBoost = pop_weight * log归一阅读量',
  `cf_blend_lambda` decimal(10,6) NOT NULL DEFAULT 0.250000 COMMENT 'Item-CF混合λ：(1-λ)*normTag + λ*normCf + popBoost',
  `cf_max_seeds` int NOT NULL DEFAULT 8 COMMENT 'Item-CF种子书数量上限',
  `cf_pure_slot_cap` int NOT NULL DEFAULT 4 COMMENT '最终列表中纯协同(无标签命中)条数上限',
  `mmr_pool_cap` int NOT NULL DEFAULT 50 COMMENT 'MMR候选池：按分排序取前N本',
  `mmr_lambda` decimal(10,6) NOT NULL DEFAULT 0.700000 COMMENT 'MMR平衡：越大越偏相关，越小越偏多样性',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主推荐算法全局配置';

INSERT INTO `recommend_main_config` (
  `id`, `pop_weight`, `cf_blend_lambda`, `cf_max_seeds`, `cf_pure_slot_cap`, `mmr_pool_cap`, `mmr_lambda`
) VALUES (
  1, 0.030000, 0.250000, 8, 4, 50, 0.700000
) ON DUPLICATE KEY UPDATE `id` = `id`;
