-- 修改 banner_carousel 表，让所有字段都可选
-- 只需要执行这个脚本，不需要重新创建表

USE novel_db;

-- 修改 novel_id 为可选
ALTER TABLE `banner_carousel` 
MODIFY COLUMN `novel_id` int NULL DEFAULT NULL COMMENT '关联的小说ID（可选）';

-- 如果存在外键约束，删除它
-- ALTER TABLE `banner_carousel` DROP FOREIGN KEY `fk_banner_novel`;

SELECT '修改完成！novel_id 现在是可选字段了' AS message;
