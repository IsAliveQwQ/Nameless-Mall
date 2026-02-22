CREATE TABLE IF NOT EXISTS `oms_flash_sale_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵 ID',
  `user_id` bigint NOT NULL COMMENT '用戶 ID',
  `promotion_id` bigint NOT NULL COMMENT '活動 ID',
  `sku_id` bigint NOT NULL COMMENT '商品 SKU ID',
  `order_sn` varchar(64) NOT NULL COMMENT '對應的訂單編號',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '購買數量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_promo_sku` (`user_id`, `promotion_id`, `sku_id`) COMMENT '用戶-活動-商品 唯一約束 (限購一次)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒殺成功記錄表 (用於 DB 層冪等與限購)';
