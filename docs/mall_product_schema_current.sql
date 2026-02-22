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
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `name` varchar(64) NOT NULL COMMENT '???迂',
  `parent_id` bigint DEFAULT '0' COMMENT '?嗅?憿D',
  `level` int DEFAULT '1' COMMENT '撅斤?',
  `icon` varchar(255) DEFAULT NULL COMMENT '疇??怏ˍ壅汀汕兩 URL',
  `sort_order` int DEFAULT '0' COMMENT '疆鬚?氐甄汕玳抽¯?疆?Ⅹ蜆氐凌阬債氐刈岔阬債怏乒兜?,
  `status` tinyint DEFAULT '1' COMMENT '癟?嫖疆?色?1=疇?〣蜆把?0=疇??癟??,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '疇罈繙癟竄?嘔色?抽?,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '疆?甄棺色冕色?抽?,
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '矇?阬撢紐瓦秉抽瞻 0=疆簫瞿疇繡繡 1=疇繚簡疇?穠矇?Ⅹ?,
  PRIMARY KEY (`id`),
  KEY `idx_categories_parent_id` (`parent_id`),
  KEY `idx_categories_sort_order` (`sort_order`),
  KEY `idx_categories_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='????銵?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '疇?Ｔ乒D',
  `url` varchar(255) NOT NULL COMMENT '疇??把售｜RL',
  `is_main` tinyint(1) DEFAULT '0' COMMENT '疆?簪疇?礎瓣繡罈疇???,
  `sort_order` int DEFAULT '0' COMMENT '疆鬚?氐甄?,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='疇?Ｔ乒汀把售￣阬¯?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_tags`
--

DROP TABLE IF EXISTS `product_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_tags` (
  `product_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`,`tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `name` varchar(128) NOT NULL COMMENT '???迂',
  `title` varchar(255) DEFAULT NULL COMMENT '疇?Ｔ乒乒兜紐汕兩矇癒?/疆?阬汕?,
  `description` text COMMENT '???膩',
  `price` decimal(10,2) NOT NULL COMMENT '?箇??寞',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '??????/??? (???????????????)',
  `stock` int DEFAULT '0' COMMENT '蝮賢澈摮?,
  `published_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '瓣繡?疆鱉繞疆?Ｔ抽?,
  `category_id` bigint DEFAULT NULL COMMENT '??ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '撱箇???',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '?湔??',
  `main_image` varchar(255) DEFAULT NULL COMMENT '疇?Ｔ乒刈蜃酵汀?URL',
  `status` tinyint DEFAULT '1' COMMENT '癟?嫖疆?色?1=瓣繡?疆鱉繞 0=瓣繡?嘔汍壇?,
  `sales` int DEFAULT '0' COMMENT '矇?繚矇?¯?,
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '矇?阬撢紐瓦秉抽瞻 0=疆簫瞿疇繡繡 1=疇繚簡疇?穠矇?Ⅹ?,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_products_status` (`status`),
  KEY `idx_products_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='??銝餉”';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL COMMENT '疆穡?〡岑授勻氐岑阬?,
  `style` varchar(64) DEFAULT NULL COMMENT '疆穡瞿疇翹?',
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_tags_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `variant_id` bigint NOT NULL COMMENT '癡礎?疆?翹ID',
  `option_name` varchar(64) NOT NULL,
  `option_value` varchar(64) NOT NULL,
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '矇?阬撢紐瓦秉抽瞻',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_variant_id` (`variant_id`),
  KEY `idx_vo_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='癡礎?疆?翹矇?繡矇??污阬¯?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `variants`
--

DROP TABLE IF EXISTS `variants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `variants` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '閬ID',
  `product_id` bigint NOT NULL COMMENT '??ID',
  `sku` varchar(64) NOT NULL COMMENT 'SKU蝺函Ⅳ',
  `price` decimal(10,2) DEFAULT NULL COMMENT '閬?寞',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '??????/??? (???????????????)',
  `stock` int DEFAULT '0' COMMENT '閬摨怠?',
  `name` varchar(64) DEFAULT NULL COMMENT '癡礎?疆?翹疇??癟穡簣 疇礎?? 矇罈?兩兜?128G',
  `image` varchar(255) DEFAULT NULL COMMENT '癡礎?疆?翹疇??把售?URL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '疇罈繙癟竄?嘔色?抽?,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '疆?甄棺色冕色?抽?,
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '矇?阬撢紐瓦秉抽瞻 0=疆簫瞿疇繡繡 1=疇繚簡疇?穠矇?Ⅹ?,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_variants_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='??閬霈?銵?;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-26  9:41:00
