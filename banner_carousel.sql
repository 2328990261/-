-- ============================================================
-- 轮播图模块：数据库表创建脚本
-- 执行前请备份 novel_db
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for banner_carousel
-- ----------------------------
DROP TABLE IF EXISTS `banner_carousel`;
CREATE TABLE `banner_carousel` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `novel_id` int NULL DEFAULT NULL COMMENT '关联的小说ID（可选）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '轮播图标题（可选，默认使用小说名）',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '自定义轮播图片URL（可选，默认使用小说封面）',
  `link_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '点击跳转链接（可选，默认跳转到小说详情）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序权重（数字越小越靠前）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始展示时间（可选，为空则立即生效）',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束展示时间（可选，为空则永久有效）',
  `click_count` int NOT NULL DEFAULT 0 COMMENT '点击次数统计',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_novel_id` (`novel_id` ASC) USING BTREE COMMENT '小说ID索引',
  INDEX `idx_status_sort` (`status` ASC, `sort_order` ASC) USING BTREE COMMENT '状态和排序组合索引',
  INDEX `idx_time_range` (`start_time` ASC, `end_time` ASC) USING BTREE COMMENT '时间范围索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '轮播图配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 插入示例数据（可选）
-- 注意：需要确保 novel_book_main 表中存在对应的 novel_id
-- ----------------------------
-- INSERT INTO `banner_carousel` (`novel_id`, `title`, `sort_order`, `status`) VALUES
-- (1, '热门推荐：公爵千金的家庭教师', 1, 1),
-- (2, '新书上架：后宫之鸟', 2, 1),
-- (3, '编辑推荐：埃罗芒阿老师', 3, 1);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 使用说明
-- ============================================================
-- 1. 基本用法：
--    - novel_id: 必填，关联到 novel_book_main 表
--    - title: 可选，不填则使用小说名
--    - image_url: 可选，不填则使用小说封面
--    - sort_order: 排序，数字越小越靠前
--    - status: 1=启用 0=禁用
--
-- 2. 高级功能：
--    - start_time/end_time: 设置轮播图的展示时间段
--    - link_url: 自定义跳转链接（如活动页面）
--    - click_count: 自动统计点击次数
--
-- 3. 查询示例：
--    -- 获取当前有效的轮播图（按排序）
--    SELECT bc.*, nbm.book_main_name, nbm.cover, nbm.author
--    FROM banner_carousel bc
--    LEFT JOIN novel_book_main nbm ON bc.novel_id = nbm.id
--    WHERE bc.status = 1
--      AND (bc.start_time IS NULL OR bc.start_time <= NOW())
--      AND (bc.end_time IS NULL OR bc.end_time >= NOW())
--    ORDER BY bc.sort_order ASC, bc.id DESC
--    LIMIT 5;
--
-- 4. 后台管理功能建议：
--    - 增删改查轮播图
--    - 拖拽排序（修改 sort_order）
--    - 启用/禁用开关
--    - 设置展示时间段
--    - 查看点击统计
-- ============================================================
