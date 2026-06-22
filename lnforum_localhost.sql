/*
 Navicat Premium Dump SQL

 Source Server         : 2023mysql
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : lnforum_localhost

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 22/06/2026 13:47:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`category_id`) USING BTREE,
  INDEX `idx_sort`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for category_tag
-- ----------------------------
DROP TABLE IF EXISTS `category_tag`;
CREATE TABLE `category_tag`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `category_id` int NOT NULL COMMENT '分类ID',
  `tag_id` int NOT NULL COMMENT '标签ID',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_tag`(`category_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_tag`(`tag_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort_order` ASC) USING BTREE,
  CONSTRAINT `category_tag_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `category_tag_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`tag_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分类-标签关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for follow
-- ----------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow`  (
  `follow_id` int NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` int NULL DEFAULT NULL COMMENT '主动ID',
  `following_id` int NULL DEFAULT NULL COMMENT '被动ID',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '0拉黑1关注',
  PRIMARY KEY (`follow_id`) USING BTREE,
  UNIQUE INDEX `uk_follower_following`(`follower_id` ASC, `following_id` ASC) USING BTREE,
  INDEX `idx_follower`(`follower_id` ASC) USING BTREE,
  INDEX `idx_following`(`following_id` ASC) USING BTREE,
  CONSTRAINT `follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `follow_ibfk_2` FOREIGN KEY (`following_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注/关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for interaction
-- ----------------------------
DROP TABLE IF EXISTS `interaction`;
CREATE TABLE `interaction`  (
  `interaction_id` int NOT NULL AUTO_INCREMENT COMMENT '互动ID',
  `post_id` int NOT NULL COMMENT '动态ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `interaction_type` enum('like','comment','favorite') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '互动类型',
  `comment_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评论内容',
  `parent_id` int NULL DEFAULT NULL COMMENT '父评论ID',
  `comment_status` enum('正常','已删除','违规') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '正常' COMMENT '评论子状态',
  `comment_like_count` int NULL DEFAULT 0 COMMENT '评论点赞数',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态：0删除，1没删',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`interaction_id`) USING BTREE,
  INDEX `idx_post_type`(`post_id` ASC, `interaction_type` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_id` ASC, `interaction_type` ASC) USING BTREE,
  INDEX `idx_post_comment`(`post_id` ASC, `interaction_type` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_interaction_composite`(`post_id` ASC, `interaction_type` ASC, `status` ASC, `create_time` DESC) USING BTREE,
  CONSTRAINT `interaction_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `interaction_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `interaction_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `interaction` (`interaction_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '互动表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `message_id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` int NULL DEFAULT NULL COMMENT '发送者ID（NULL表示系统消息）',
  `receiver_id` int NULL DEFAULT NULL COMMENT '接收者ID（系统通知时可为NULL，通过message_receiver表管理）',
  `message_type` tinyint(1) NULL DEFAULT 0 COMMENT '类型：0私信，1系统通知',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `msg_format` tinyint(1) NULL DEFAULT 0 COMMENT '格式：0text，1img',
  `image_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片地址',
  `related_type` enum('用户','帖子','评论','订单','任务','举报') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联类型(保留作为参考)',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`message_id`) USING BTREE,
  INDEX `idx_receiver_type`(`receiver_id` ASC, `message_type` ASC) USING BTREE,
  INDEX `idx_receiver_read`(`receiver_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `message_ibfk_1`(`sender_id` ASC) USING BTREE,
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `message_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message_receiver
-- ----------------------------
DROP TABLE IF EXISTS `message_receiver`;
CREATE TABLE `message_receiver`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `message_id` int NOT NULL COMMENT '消息ID',
  `receiver_id` int NOT NULL COMMENT '接收者ID',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_message_receiver`(`message_id` ASC, `receiver_id` ASC) USING BTREE COMMENT '同一消息同一接收者唯一',
  INDEX `idx_receiver_read`(`receiver_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_message_id`(`message_id` ASC) USING BTREE,
  CONSTRAINT `fk_mr_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`message_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_mr_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息接收者关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for post
-- ----------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post`  (
  `post_id` int NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` int NOT NULL COMMENT '发布者ID',
  `category_id` int NOT NULL COMMENT '分类ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动态内容',
  `contact_info` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '隐私信息',
  `deadline` datetime NULL DEFAULT NULL COMMENT '截至时间',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  `item_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '物品信息',
  `start_point` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '起点',
  `end_point` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '终点',
  `image1` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片1',
  `image2` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片2',
  `image3` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片3',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0待审核, 1已通过, 2已删除, 3已结束，4未通过，5进行中',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `favorite_count` int NULL DEFAULT 0 COMMENT '收藏数',
  `trending_level` tinyint(1) NULL DEFAULT 0 COMMENT '热度：0普通, 1热门',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`post_id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_category_time`(`category_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_trending_time`(`trending_level` ASC, `create_time` DESC) USING BTREE,
  CONSTRAINT `post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `post_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for post_tag
-- ----------------------------
DROP TABLE IF EXISTS `post_tag`;
CREATE TABLE `post_tag`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `post_id` int NOT NULL COMMENT '动态ID',
  `tag_id` int NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_tag`(`post_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_tag`(`tag_id` ASC) USING BTREE,
  INDEX `idx_post`(`post_id` ASC) USING BTREE,
  CONSTRAINT `post_tag_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `post_tag_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`tag_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态-标签关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for report
-- ----------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report`  (
  `report_id` int NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_id` int NOT NULL COMMENT '举报人ID',
  `target_type` enum('用户','帖子','评论') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报目标类型',
  `interaction` int NULL DEFAULT NULL COMMENT '被举报评论的id',
  `post_id` int NULL DEFAULT NULL COMMENT '被举报动态的id',
  `target_id` int NULL DEFAULT NULL COMMENT '举报目标ID',
  `report_type` enum('垃圾信息','色情低俗','违法违规','欺诈','侵权','其他') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报类型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报描述',
  `report_image` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '举报图片地址',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0待处理, 1已处理',
  `admin_id` int NULL DEFAULT NULL COMMENT '处理管理员ID',
  `result` tinyint(1) NULL DEFAULT NULL COMMENT '结果：0未通过, 1通过',
  `feedback` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理反馈',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  `process_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`report_id`) USING BTREE,
  UNIQUE INDEX `uk_reporter_target`(`reporter_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `report_ibfk_3`(`post_id` ASC) USING BTREE,
  INDEX `report_ibfk_4`(`interaction` ASC) USING BTREE,
  CONSTRAINT `report_ibfk_1` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `report_ibfk_2` FOREIGN KEY (`admin_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `report_ibfk_3` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `report_ibfk_4` FOREIGN KEY (`interaction`) REFERENCES `interaction` (`interaction_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '举报表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for system_setting
-- ----------------------------
DROP TABLE IF EXISTS `system_setting`;
CREATE TABLE `system_setting`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `site_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '小宁论坛' COMMENT '站点名称',
  `icp_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '备案号',
  `site_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '站点状态：1开启，0维护中',
  `allow_register` tinyint(1) NOT NULL DEFAULT 1 COMMENT '开放注册：1允许，0禁止',
  `sensitive_words` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '敏感词过滤，逗号分隔',
  `max_image_size` int NOT NULL DEFAULT 5 COMMENT '最大图片大小(MB)',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统全站配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `tag_id` int NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`tag_id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标签表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for trade_task
-- ----------------------------
DROP TABLE IF EXISTS `trade_task`;
CREATE TABLE `trade_task`  (
  `task_id` int NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编号',
  `post_id` int NOT NULL COMMENT '关联动态ID',
  `creator_id` int NOT NULL COMMENT '创建者ID',
  `acceptor_id` int NULL DEFAULT NULL COMMENT '接受者ID',
  `task_type` enum('二手交易','跑腿服务') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务描述',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额/报酬',
  `start_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '起点',
  `end_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '终点',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `estimated_time` int NULL DEFAULT NULL COMMENT '预计耗时(分钟)',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `task_status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0进行中, 1已完成, 2已取消',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '取消原因',
  PRIMARY KEY (`task_id`) USING BTREE,
  UNIQUE INDEX `task_no`(`task_no` ASC) USING BTREE,
  UNIQUE INDEX `post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_task_status`(`task_status` ASC) USING BTREE,
  INDEX `idx_trade_task_composite`(`task_type` ASC, `task_status` ASC, `create_time` DESC) USING BTREE,
  INDEX `trade_task_ibfk_2`(`creator_id` ASC) USING BTREE,
  INDEX `trade_task_ibfk_3`(`acceptor_id` ASC) USING BTREE,
  CONSTRAINT `trade_task_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `trade_task_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `trade_task_ibfk_3` FOREIGN KEY (`acceptor_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '交易任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `phone` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'default_avatar.png' COMMENT '头像URL',
  `gender` tinyint(1) NULL DEFAULT 0 COMMENT '性别：0未知, 1男, 2女',
  `signature` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个性签名',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `role` tinyint(1) NULL DEFAULT 3 COMMENT '角色：0超级管理员, 1管理员, 2跑腿员, 3普通用户',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0正常, 1禁用, 2注销',
  `warning_count` int NULL DEFAULT 0 COMMENT '警告次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_ban_history
-- ----------------------------
DROP TABLE IF EXISTS `user_ban_history`;
CREATE TABLE `user_ban_history`  (
  `history_id` int NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `admin_id` int NOT NULL COMMENT '操作管理员ID',
  `action_type` enum('封禁','解封') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `restrictions_before` json NULL COMMENT '封禁前权限',
  `restrictions_after` json NULL COMMENT '封禁后权限',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作原因',
  `duration_days` int NULL DEFAULT 0 COMMENT '封禁天数（0为永久）',
  `start_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否生效',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`history_id`) USING BTREE,
  INDEX `admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `idx_user_active`(`user_id` ASC, `is_active` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_end_time`(`end_time` ASC) USING BTREE,
  CONSTRAINT `user_ban_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `user_ban_history_ibfk_2` FOREIGN KEY (`admin_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户封禁历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_permission
-- ----------------------------
DROP TABLE IF EXISTS `user_permission`;
CREATE TABLE `user_permission`  (
  `permission_id` int NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `can_post` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以发帖',
  `can_comment` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以评论',
  `can_like` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以点赞',
  `can_follow` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以关注',
  `can_message` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以发送私信',
  `can_buy` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以购买',
  `can_sell` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以出售',
  `can_run_errand` tinyint(1) NULL DEFAULT 1 COMMENT '是否可以跑腿',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_can_post`(`can_post` ASC) USING BTREE,
  INDEX `idx_can_comment`(`can_comment` ASC) USING BTREE,
  CONSTRAINT `user_permission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_setting
-- ----------------------------
DROP TABLE IF EXISTS `user_setting`;
CREATE TABLE `user_setting`  (
  `setting_id` int NOT NULL AUTO_INCREMENT COMMENT '设置ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `theme` tinyint(1) NULL DEFAULT 0 COMMENT '主题：0light, 1dark',
  `notification_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '接收通知',
  `private_message_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '接收私信',
  `language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'zh-CN' COMMENT '语言',
  `privacy_level` enum('公开','好友可见','仅自己') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '公开' COMMENT '隐私级别',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`setting_id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `user_setting_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户设置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Triggers structure for table user
-- ----------------------------
DROP TRIGGER IF EXISTS `after_user_insert`;
delimiter ;;
CREATE TRIGGER `after_user_insert` AFTER INSERT ON `user` FOR EACH ROW BEGIN
    
    INSERT IGNORE INTO `user_permission` (user_id) VALUES (NEW.user_id);
    
    INSERT IGNORE INTO `user_setting` (user_id) VALUES (NEW.user_id);
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
