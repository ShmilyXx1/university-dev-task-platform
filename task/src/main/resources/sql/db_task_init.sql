-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: localhost    Database: db_task
-- ------------------------------------------------------
-- Server version	8.0.33

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `db_task`
--

/*!40000 DROP DATABASE IF EXISTS `db_task`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `db_task` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `db_task`;

--
-- Table structure for table `t_chat_message`
--

DROP TABLE IF EXISTS `t_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_chat_message` (
  `message_id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `from_user_id` int NOT NULL COMMENT '发送者ID',
  `to_user_id` int NOT NULL COMMENT '接收者ID',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `send_datetime` datetime NOT NULL COMMENT '发送时间',
  `msg_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'chat',
  `is_read` tinyint DEFAULT '0',
  PRIMARY KEY (`message_id`),
  KEY `idx_to_read` (`to_user_id`,`is_read`),
  KEY `idx_from_to` (`from_user_id`,`to_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服聊天消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_chat_message`
--

LOCK TABLES `t_chat_message` WRITE;
/*!40000 ALTER TABLE `t_chat_message` DISABLE KEYS */;
INSERT INTO `t_chat_message` VALUES (1,11,13,'113131','2026-09-10 16:35:46','chat',1),(2,13,11,'2312xz','2026-09-10 16:35:53','chat',1),(3,13,11,'13131','2026-09-10 16:36:13','chat',1),(4,13,12,'13413','2026-09-10 16:36:44','chat',0),(5,12,13,'131','2026-09-10 16:36:58','chat',1),(6,13,11,'`13131','2026-09-10 17:02:21','chat',1),(7,11,13,'1','2026-09-10 17:02:30','chat',1),(8,13,11,'11','2026-09-10 17:02:36','chat',1),(9,12,13,'4我','2026-09-10 17:05:21','chat',1),(10,12,13,'区阿尔阿','2026-09-10 17:05:38','chat',1),(11,2,11,'111','2026-09-12 14:26:23','chat',1),(12,11,2,'sa','2026-09-12 14:26:50','chat',1),(13,2,11,'dsda','2026-09-12 14:27:08','chat',0);
/*!40000 ALTER TABLE `t_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_feedback`
--

DROP TABLE IF EXISTS `t_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_feedback` (
  `feedback_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `solve` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `send_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `solve_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `reply` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客服回复内容',
  `reply_datetime` datetime DEFAULT NULL COMMENT '客服回复时间',
  PRIMARY KEY (`feedback_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_feedback`
--

LOCK TABLES `t_feedback` WRITE;
/*!40000 ALTER TABLE `t_feedback` DISABLE KEYS */;
INSERT INTO `t_feedback` VALUES (2,2,'这个软件非常垃圾','0','2026-05-26 15:57:08','2026-05-26 17:11:55','测试回复内容','2026-09-12 15:05:19'),(3,2,'xxxxxx','1','2026-05-26 15:57:25','2026-05-26 17:17:55',NULL,NULL),(5,12,'academic','0','2026-09-10 17:06:28','2026-09-10 17:06:28','testreply2','2026-09-12 14:51:31');
/*!40000 ALTER TABLE `t_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_order`
--

DROP TABLE IF EXISTS `t_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_order` (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `sender_id` int NOT NULL,
  `getter_id` int DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `deposit` decimal(10,2) NOT NULL,
  `sender_price` decimal(10,2) NOT NULL,
  `state` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `user_complete` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0',
  `document_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `auditor_complete` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0',
  `release_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `complete_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `pay_state` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '赏金支付状态：0未托管 1已托管 2已结算 3已退款',
  `deposit_state` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '押金支付状态：0未支付 1已支付 2已退还',
  `pay_trade_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '赏金支付宝流水号',
  `deposit_trade_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '押金支付宝流水号',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_order`
--

LOCK TABLES `t_order` WRITE;
/*!40000 ALTER TABLE `t_order` DISABLE KEYS */;
INSERT INTO `t_order` VALUES (3,2,2,'3','支持XZ从ZX','学习',12.00,30.00,'1','1','c','0','2026-05-09 17:36:27','2026-05-09 17:36:27',NULL,'2026-09-08 09:36:28','1','0',NULL,NULL),(10,3,NULL,'3','徐徐','编程',200.00,8.00,'0','0',NULL,'0','2026-05-26 18:06:09','2026-05-26 18:06:09','2026-05-26 18:06:09',NULL,'0','0',NULL,NULL),(12,2,11,'zxx','啊大大大大大大','学习',300.00,70.00,'1','0',NULL,'0','2026-08-19 15:18:54','2026-08-29 00:00:00','2026-08-19 15:18:54',NULL,'0','0',NULL,NULL),(13,11,NULL,'JAVA Web项目','制作一个线上订书系统,需要完成的功能等,请与发单人联系','编程',210.00,300.00,'0','0',NULL,'0','2026-08-21 16:05:15','2026-09-05 00:00:00','2026-08-21 16:05:15',NULL,'0','1',NULL,'2026090322001411310508951472'),(14,11,0,'微信小程序开发','开发微信小程序,游戏类的小程序','编程',1000.00,1000.00,'0','1','www.123.com','0','2026-08-25 19:40:35','2026-09-16 00:00:00','2026-08-25 19:40:35',NULL,'0','0',NULL,'2026090322001411310508952615'),(16,2,11,'测试','测试soft','设计',800.00,200.00,'2','1','www.4399.com','1','2026-08-25 21:19:56','2026-08-31 00:00:00','2026-09-10 11:38:15','2026-09-10 11:38:15','2','2','2026090922001411310508974763','2026091022001411310508981424'),(17,2,11,'C++','131313132','设计',500.00,11111.00,'2','1','www.baidu.com','1','2026-09-03 16:03:01','2026-09-30 00:00:00','2026-09-03 16:46:41','2026-09-03 16:46:41','2','2','2026090322001411310508950321','2026090322001411310508950322'),(23,2,0,'saxcxzz','sdxxxxxxxx','学习',500.00,70.00,'0','0',NULL,'0','2026-09-09 10:02:17','2026-09-30 00:00:00','2026-09-09 10:02:17',NULL,'1','0','2026090922001411310508974762',NULL),(24,11,0,'Spring Cloud ','dsazczxczc','设计',300.00,200.00,'0','0',NULL,'0','2026-09-09 11:29:34','2026-09-27 00:00:00','2026-09-09 11:29:34',NULL,'1','0','2026090922001411310508976719',NULL);
/*!40000 ALTER TABLE `t_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_payment`
--

DROP TABLE IF EXISTS `t_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_payment` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` int NOT NULL COMMENT '订单ID',
  `user_id` int NOT NULL COMMENT '付款用户ID',
  `pay_type` varchar(16) NOT NULL COMMENT '支付类型：BOUNTY赏金 DEPOSIT押金',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `out_trade_no` varchar(64) NOT NULL COMMENT '商户订单号（自己生成）',
  `trade_no` varchar(64) DEFAULT NULL COMMENT '支付宝流水号',
  `state` varchar(2) DEFAULT '0' COMMENT '状态：0待支付 1支付成功 2已关闭',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_order_type` (`order_id`,`pay_type`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_payment`
--

LOCK TABLES `t_payment` WRITE;
/*!40000 ALTER TABLE `t_payment` DISABLE KEYS */;
INSERT INTO `t_payment` VALUES (2,14,2,'DEPOSIT',1000.00,'DEPOSIT_14_2_1788422167709','2026090322001411310508952615','3','2026-09-03 15:56:08','2026-09-03 15:56:56'),(3,17,2,'BOUNTY',11111.00,'BOUNTY_17_2_1788422652996','2026090322001411310508950321','1','2026-09-03 16:04:13','2026-09-03 16:05:29'),(4,17,11,'DEPOSIT',500.00,'DEPOSIT_17_11_1788424726222','2026090322001411310508950322','3','2026-09-03 16:38:46','2026-09-03 16:39:24'),(5,17,11,'SETTLE',11111.00,'SETTLE_17_1788425204699','BOOK_SETTLE','1','2026-09-03 16:46:45','2026-09-03 16:46:45'),(6,22,2,'BOUNTY',90.00,'BOUNTY_22_2_1788861614782',NULL,'0','2026-09-08 18:00:15',NULL),(7,23,2,'BOUNTY',70.00,'BOUNTY_23_2_1788919769087','2026090922001411310508974762','1','2026-09-09 10:09:29','2026-09-09 10:10:16'),(8,16,2,'BOUNTY',200.00,'BOUNTY_16_2_1788919835614',NULL,'0','2026-09-09 10:10:36',NULL),(9,16,2,'BOUNTY',200.00,'BOUNTY_16_2_1788919945301','2026090922001411310508974763','1','2026-09-09 10:12:25','2026-09-09 10:13:07'),(10,24,11,'BOUNTY',200.00,'BOUNTY_24_11_1788924577678','2026090922001411310508976719','1','2026-09-09 11:29:38','2026-09-09 11:31:09'),(11,16,11,'DEPOSIT',800.00,'DEPOSIT_16_11_1789010891696','2026091022001411310508981424','3','2026-09-10 11:28:12','2026-09-10 11:28:50'),(12,16,11,'SETTLE',200.00,'SETTLE_16_1789011497235','BOOK_SETTLE','1','2026-09-10 11:38:17','2026-09-10 11:38:17');
/*!40000 ALTER TABLE `t_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_role`
--

DROP TABLE IF EXISTS `t_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_role`
--

LOCK TABLES `t_role` WRITE;
/*!40000 ALTER TABLE `t_role` DISABLE KEYS */;
INSERT INTO `t_role` VALUES (1,'管理员'),(2,'审核员'),(3,'客服'),(4,'普通用户');
/*!40000 ALTER TABLE `t_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `register_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_datetime` datetime DEFAULT NULL,
  `state` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user`
--

LOCK TABLES `t_user` WRITE;
/*!40000 ALTER TABLE `t_user` DISABLE KEYS */;
INSERT INTO `t_user` VALUES (2,'xxx','$2a$10$gGeUnUA2Dj2pDoC7uL0cI.1q8FLmV3oQRyjAF0RoVBzvUx9zNSni2','19848117279','男',21,NULL,'湖南省长沙市长沙县','','2026-05-07 20:07:14','xz','2026-06-02 09:12:31','0'),(11,'zzzzz','$2a$10$SsMCPtyrAZ2T5Oqf1tDXqO3EEHdjs8L3iO4ZaAk7.IG.RwzLcLmcO','13467998199','男',22,'5121545@qq.com','上海市嘉定区','','2026-08-21 14:39:11','sadad','2026-08-21 15:54:51','0'),(12,'auditorTest','$2a$10$Gp8y7Z5uFsVsOAxI7qsPAO9gdEQAZC99NjOBgJqk.lh.W73t/XQrq','19900005678',NULL,0,NULL,NULL,'/avatar/default-avatar.jpg','2026-09-03 16:46:09',NULL,NULL,'0'),(13,'客服001','$2a$10$fDbpj52417.g5mI7rDGyf.lQon/0gC2TYVUX52tBaaYlOSlk1r5bm','19251737711','男',25,'xzczca@qq.com','新疆市','/avatar/default-avatar.jpg','2026-09-10 16:18:07','lz',NULL,'0');
/*!40000 ALTER TABLE `t_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user_role`
--

DROP TABLE IF EXISTS `t_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user_role` (
  `user_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user_role`
--

LOCK TABLES `t_user_role` WRITE;
/*!40000 ALTER TABLE `t_user_role` DISABLE KEYS */;
INSERT INTO `t_user_role` VALUES (2,1),(2,4),(11,4),(12,2),(12,4),(13,3),(13,4);
/*!40000 ALTER TABLE `t_user_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-14 12:11:19
