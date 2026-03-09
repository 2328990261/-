-- 给user_preference_tag表添加tag_type字段，区分收藏标签和自定义标签
ALTER TABLE `user_preference_tag` 
ADD COLUMN `tag_type` VARCHAR(20) NOT NULL DEFAULT 'collection' COMMENT '标签类型：collection=收藏排序，custom=自定义排序' AFTER `tag_name`;

-- 添加索引
ALTER TABLE `user_preference_tag` 
ADD KEY `idx_user_tag_type` (`user_id`, `tag_type`);
