/*
  每用户标签权重覆盖配置：
  - 用户可对部分/全部标签设置自定义权重（0~1之间建议值，但不强制）
  - 未配置的标签推荐时会用动态计算值补齐
*/

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_tag_weight` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '外键 user.id',
  `tag_name` varchar(50) NOT NULL COMMENT '标签名',
  `weight` decimal(10,6) NOT NULL COMMENT '用户配置的标签权重（覆盖项）',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag_weight` (`user_id`, `tag_name`),
  CONSTRAINT `fk_user_tag_weight_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户标签权重覆盖配置（每用户一套）';

