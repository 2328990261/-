-- ============================================================
-- 后台管理模块：数据库变更脚本
-- 执行前请备份 novel_db
-- ============================================================

-- 1. novel_book_main 增加上下架状态（0=下架 1=上架），默认上架以兼容已有数据
ALTER TABLE `novel_book_main`
ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '0=下架 1=上架' AFTER `read_count`;

-- 2. user 表增加账号状态（0=正常 1=禁用）
ALTER TABLE `user`
ADD COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=正常 1=禁用' AFTER `is_admin`;

-- 3. 平台标签表（管理员可填删改查，推荐权重用于推荐配置）
CREATE TABLE IF NOT EXISTS `tag` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '标签名',
  `recommend_weight` decimal(5,2) NOT NULL DEFAULT 0.10 COMMENT '推荐权重',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '管理端排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台标签表';

-- 初始标签与前端标签栏（LibraryTagSearch.vue / 后端 Mapper）一致，便于前后端联调
-- 第1行：日常/奇幻/校园/冒险/异世界
-- 第2行：轻松/搞笑/治愈/致郁/甜宠/热血/恋爱/成长
-- 第3行：智斗/悬疑/推理/心理惊悚/战斗/竞技/基建
-- 第4行：宫廷/虚拟网游/现实题材/种田文/转生/穿越/魔法
-- 第5行：全年龄/轻百
INSERT IGNORE INTO `tag` (`name`, `recommend_weight`, `sort_order`) VALUES
('日常', 0.40, 1),
('奇幻', 0.38, 2),
('校园', 0.36, 3),
('冒险', 0.36, 4),
('异世界', 0.38, 5),
('轻松', 0.28, 6),
('搞笑', 0.26, 7),
('治愈', 0.28, 8),
('致郁', 0.24, 9),
('甜宠', 0.28, 10),
('热血', 0.28, 11),
('恋爱', 0.30, 12),
('成长', 0.26, 13),
('智斗', 0.24, 14),
('悬疑', 0.26, 15),
('推理', 0.26, 16),
('心理惊悚', 0.22, 17),
('战斗', 0.26, 18),
('竞技', 0.24, 19),
('基建', 0.24, 20),
('宫廷', 0.24, 21),
('虚拟网游', 0.26, 22),
('现实题材', 0.24, 23),
('种田文', 0.24, 24),
('转生', 0.28, 25),
('穿越', 0.28, 26),
('魔法', 0.26, 27),
('全年龄', 0.20, 28),
('轻百', 0.22, 29);
