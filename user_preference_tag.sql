-- 用户阅读偏好标签表
CREATE TABLE IF NOT EXISTS `user_preference_tag` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT(11) NOT NULL COMMENT '用户ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `tag_order` INT(11) NOT NULL DEFAULT 0 COMMENT '标签排序（自定义排序用）',
  `is_first_row` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否在第一行（收藏排序用，1=是，0=否）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tag_order` (`tag_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户阅读偏好标签表';
