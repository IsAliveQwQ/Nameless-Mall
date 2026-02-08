package com.nameless.mall.order.mq;

import com.nameless.mall.order.config.RabbitMQConfig;
import com.nameless.mall.order.service.ReliableMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 訂單消息生產者
 * 負責發送訂單創建、延遲取消等消息。
 */
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final ReliableMessageService reliableMessageService;

    /**
     * 發送訂單創建消息 (主要供搜尋服務更新銷量)
     */
    public void sendOrderCreated(Long orderId, List<Long> productIds) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderId", orderId);
        message.put("productIds", productIds);
        message.put("timestamp", System.currentTimeMillis());

        log.info("【MQ】發送訂單創建消息: orderId={}", orderId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_ORDER_CREATED, message);
    }

    /**
     * 發送訂單延遲取消偵測消息 (TTL + DLX)
     */
    public void sendOrderDelay(String orderSn) {
        log.info("【MQ】發送訂單延遲取消偵測消息: orderSn={}", orderSn);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EVENT_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                orderSn);
    }

    /**
     * 發送訂單取消事件 (供下游服務如 Promotion Service 釋放資源)
     * 使用本地訊息表 (Transactional Outbox) 模式，確保消息可靠落庫。
     * 後續由 MessageRelayTask 異步發送。
     */
    public void sendOrderCancelled(String orderSn) {
        log.info("【MQ】準備發送訂單取消事件 (Outbox): orderSn={}", orderSn);
        reliableMessageService.createOrderCancelledMessage(orderSn);
    }
}
