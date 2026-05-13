-- 已有库升级：为 user_recommend_profile 增加 MMR 开关（执行一次即可）
SET NAMES utf8mb4;

ALTER TABLE `user_recommend_profile`
  ADD COLUMN `mmr_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否对结果做MMR多样性重排：0 否 1 是' AFTER `explain_enabled`;
