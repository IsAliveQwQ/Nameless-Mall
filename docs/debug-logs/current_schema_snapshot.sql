-- MySQL dump 10.13  Distrib 8.4.0, for Linux (x86_64)
--
-- Host: localhost    Database: mall_product
-- ------------------------------------------------------
-- Server version	8.4.0

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
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分類ID',
  `name` varchar(64) NOT NULL COMMENT '分類名稱',
  `parent_id` bigint DEFAULT '0' COMMENT '父分類ID',
  `level` int DEFAULT '1' COMMENT '層級',
  `icon` varchar(255) DEFAULT NULL COMMENT 'åˆ†é¡žåœ–æ¨™ URL',
  `sort_order` int DEFAULT '0' COMMENT 'æŽ’åºæ¬Šé‡ æ•¸å­—è¶Šå¤§è¶Šé å‰',
  `status` tinyint DEFAULT '1' COMMENT 'ç‹€æ…‹ 1=å•Ÿç”¨ 0=åœç”¨',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'å»ºç«‹æ™‚é–“',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ™‚é–“',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'é‚è¼¯åˆªé™¤ 0=æ­£å¸¸ 1=å·²åˆªé™¤',
  PRIMARY KEY (`id`),
  KEY `idx_categories_parent_id` (`parent_id`),
  KEY `idx_categories_sort_order` (`sort_order`),
  KEY `idx_categories_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分類表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT 'å•†å“ID',
  `url` varchar(255) NOT NULL COMMENT 'åœ–ç‰‡URL',
  `is_main` tinyint(1) DEFAULT '0' COMMENT 'æ˜¯å¦ä¸»åœ–',
  `sort_order` int DEFAULT '0' COMMENT 'æŽ’åº',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å•†å“åœ–ç‰‡è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(128) NOT NULL COMMENT '商品名稱',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '基礎價格',
  `stock` int DEFAULT '0' COMMENT '總庫存',
  `category_id` bigint DEFAULT NULL COMMENT '分類ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `main_image` varchar(255) DEFAULT NULL COMMENT 'å•†å“ä¸»åœ– URL',
  `status` tinyint DEFAULT '1' COMMENT 'ç‹€æ…‹ 1=ä¸Šæž¶ 0=ä¸‹æž¶',
  `sales` int DEFAULT '0' COMMENT 'éŠ·é‡',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'é‚è¼¯åˆªé™¤ 0=æ­£å¸¸ 1=å·²åˆªé™¤',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_products_status` (`status`),
  KEY `idx_products_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `undo_log`
--

DROP TABLE IF EXISTS `undo_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `undo_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `variant_options`
--

DROP TABLE IF EXISTS `variant_options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `variant_options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `variant_id` bigint NOT NULL COMMENT 'è¦æ ¼ID',
  `option_name` varchar(64) NOT NULL,
  `option_value` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_variant_id` (`variant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='è¦æ ¼é¸é …è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `variants`
--

DROP TABLE IF EXISTS `variants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `variants` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '規格ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku` varchar(64) NOT NULL COMMENT 'SKU編碼',
  `price` decimal(10,2) DEFAULT NULL COMMENT '規格價格',
  `stock` int DEFAULT '0' COMMENT '規格庫存',
  `name` varchar(64) DEFAULT NULL COMMENT 'è¦æ ¼åç¨± å¦‚: é»‘è‰² 128G',
  `image` varchar(255) DEFAULT NULL COMMENT 'è¦æ ¼åœ–ç‰‡ URL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'å»ºç«‹æ™‚é–“',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ™‚é–“',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'é‚è¼¯åˆªé™¤ 0=æ­£å¸¸ 1=å·²åˆªé™¤',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_variants_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品規格變體表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-26  6:14:13
