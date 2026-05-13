-- 已有 novel_db 库、仅需补「推荐行为权重」表时执行本脚本（不删表）
CREATE TABLE IF NOT EXISTS `recommend_comment_config`  (
  `id` int NOT NULL COMMENT '主键，固定单行 id=1',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否将评论热度纳入推荐分：0否 1是',
  `comment_weight` decimal(10,6) NOT NULL DEFAULT 0.020000 COMMENT '每条有效评论对推荐分的加成系数',
  `comment_count_cap` int NOT NULL DEFAULT 50 COMMENT '参与加分的评论条数上限（防刷评）',
  `read_history_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否将阅读历史纳入推荐：0否 1是',
  `read_history_weight` decimal(10,6) NOT NULL DEFAULT 0.030000 COMMENT '命中最近阅读记录的小说加分系数',
  `read_history_recent_limit` int NOT NULL DEFAULT 5 COMMENT '仅取用户最近N条阅读记录',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推荐-行为权重配置（评论+阅读历史）' ROW_FORMAT = Dynamic;

INSERT IGNORE INTO `recommend_comment_config` (`id`, `enabled`, `comment_weight`, `comment_count_cap`, `read_history_enabled`, `read_history_weight`, `read_history_recent_limit`) VALUES (1, 1, 0.020000, 50, 1, 0.030000, 5);
