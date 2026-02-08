package com.nameless.mall.product.mq;

import com.nameless.mall.product.config.RabbitMQConfig;
import com.nameless.mall.product.event.ProductSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品同步訊息發送者
 * 監聽內部事件並將其轉換為 RabbitMQ 訊息
 * 使用 TransactionalEventListener 確保數據一致性 (Best Practice)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 在事務提交後發送消息 (AFTER_COMMIT)
     * 理由：避免因為資料庫回滾導致消息發出但資料未存入的問題
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductSyncEvent(ProductSyncEvent event) {
        log.info("【MQ】事務提交，準備發送商品同步消息: [{} - {}]", event.getAction(), event.getProductId());

        String routingKey = "product.sync." + event.getAction().toLowerCase();

        Map<String, Object> message = new HashMap<>();
        message.put("productId", event.getProductId());
        message.put("action", event.getAction());
        message.put("timestamp", System.currentTimeMillis());

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_PRODUCT_TOPIC, routingKey, message);
            log.info("【MQ】消息發送成功: RoutingKey={}", routingKey);
        } catch (Exception e) {
            log.error("【MQ】發送商品同步消息失敗，ID: {}", event.getProductId(), e);
            // 此處可擴充發送失敗的補償處理 (如寫入錯誤表)
        }
    }
}
