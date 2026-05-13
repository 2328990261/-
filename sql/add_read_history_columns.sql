-- 已有 recommend_comment_config 表、需补阅读历史相关字段时执行（执行一次即可）
ALTER TABLE `recommend_comment_config`
  ADD COLUMN `read_history_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否将阅读历史纳入推荐：0否 1是' AFTER `comment_count_cap`,
  ADD COLUMN `read_history_weight` decimal(10,6) NOT NULL DEFAULT 0.030000 COMMENT '命中最近阅读记录的小说加分系数' AFTER `read_history_enabled`,
  ADD COLUMN `read_history_recent_limit` int NOT NULL DEFAULT 5 COMMENT '仅取用户最近N条阅读记录' AFTER `read_history_weight`;
