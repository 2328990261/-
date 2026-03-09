/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 90500
 Source Host           : localhost:3306
 Source Schema         : novel_db

 Target Server Type    : MySQL
 Target Server Version : 90500
 File Encoding         : 65001

 Date: 01/03/2026 16:47:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for novel_book_main
-- ----------------------------
DROP TABLE IF EXISTS `novel_book_main`;
CREATE TABLE `novel_book_main`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主书籍ID（主键）',
  `book_main_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件夹名称（主书籍名）',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '未知作者' COMMENT '作者（从EPUB文件名提取）',
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT ' 小说标签（核心 + 特色）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小说封面文件名',
  `read_count` int NULL DEFAULT 0 COMMENT '小说阅读量，用于热门推荐排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_book_main_name`(`book_main_name` ASC) USING BTREE COMMENT '避免重复入库'
) ENGINE = InnoDB AUTO_INCREMENT = 145 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小说主表（按文件夹）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_book_volume
-- ----------------------------
DROP TABLE IF EXISTS `novel_book_volume`;
CREATE TABLE `novel_book_volume`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分卷ID（主键）',
  `main_book_id` int NOT NULL COMMENT '关联主书籍ID（外键）',
  `volume_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分卷名称（EPUB文件名）',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'EPUB解析后的完整内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_main_book_id`(`main_book_id` ASC) USING BTREE COMMENT '提升关联查询效率',
  CONSTRAINT `novel_book_volume_ibfk_1` FOREIGN KEY (`main_book_id`) REFERENCES `novel_book_main` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1479 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小说分卷表（EPUB文件）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(BCrypt加密)',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `is_admin` int NULL DEFAULT 0 COMMENT '是否为管理员(0:否, 1:是)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_collection
-- ----------------------------
DROP TABLE IF EXISTS `user_collection`;
CREATE TABLE `user_collection`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `novel_id` int NOT NULL COMMENT '小说ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_novel`(`user_id` ASC, `novel_id` ASC) USING BTREE COMMENT '确保用户不会重复收藏同一本小说',
  INDEX `novel_id`(`novel_id` ASC) USING BTREE,
  CONSTRAINT `user_collection_ibfk_1` FOREIGN KEY (`novel_id`) REFERENCES `novel_book_main` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_comment
-- ----------------------------
DROP TABLE IF EXISTS `user_comment`;
CREATE TABLE `user_comment`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `novel_id` int NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `novel_id`(`novel_id` ASC) USING BTREE,
  CONSTRAINT `user_comment_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_comment_ibfk_2` FOREIGN KEY (`novel_id`) REFERENCES `novel_book_main` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户评论记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_finished_novel
-- ----------------------------
DROP TABLE IF EXISTS `user_finished_novel`;
CREATE TABLE `user_finished_novel`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `novel_id` int NOT NULL,
  `finished_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `novel_id` ASC) USING BTREE,
  INDEX `novel_id`(`novel_id` ASC) USING BTREE,
  INDEX `user_id_2`(`user_id` ASC) USING BTREE,
  CONSTRAINT `user_finished_novel_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_finished_novel_ibfk_2` FOREIGN KEY (`novel_id`) REFERENCES `novel_book_main` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户完读记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_preference_tag
-- ----------------------------
DROP TABLE IF EXISTS `user_preference_tag`;
CREATE TABLE `user_preference_tag`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tag_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'collection' COMMENT '标签类型：collection=收藏排序，custom=自定义排序',
  `tag_order` int NOT NULL,
  `is_first_row` tinyint(1) NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `tag_name` ASC) USING BTREE,
  INDEX `idx_user_tag_type`(`user_id` ASC, `tag_type` ASC) USING BTREE,
  CONSTRAINT `user_preference_tag_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 75 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户阅读偏好标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_reading_history
-- ----------------------------
DROP TABLE IF EXISTS `user_reading_history`;
CREATE TABLE `user_reading_history`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `novel_id` int NOT NULL,
  `chapter_id` int NULL DEFAULT NULL,
  `read_duration` int NULL DEFAULT 0,
  `read_progress` int NULL DEFAULT 0,
  `last_read_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `novel_id`(`novel_id` ASC) USING BTREE,
  CONSTRAINT `user_reading_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_reading_history_ibfk_2` FOREIGN KEY (`novel_id`) REFERENCES `novel_book_main` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户阅读历史和时长表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
