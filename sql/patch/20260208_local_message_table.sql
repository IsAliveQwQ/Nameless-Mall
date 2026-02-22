CREATE TABLE IF NOT EXISTS `local_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主鍵 ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息唯一識別碼 (UUID)',
  `content` TEXT NOT NULL COMMENT '消息內容 (JSON)',
  `exchange` VARCHAR(100) NOT NULL COMMENT '交換機',
  `routing_key` VARCHAR(100) NOT NULL COMMENT '路由鍵',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '狀態: 0-新建, 1-已發送, 2-發送失敗, 3-已死亡',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重試次數',
  `max_retry` INT NOT NULL DEFAULT 3 COMMENT '最大重試次數',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重試時間',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地訊息表';
