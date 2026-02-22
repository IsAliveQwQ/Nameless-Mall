package com.nameless.mall.order.mq;

import com.nameless.mall.order.config.RabbitMQConfig;
import com.nameless.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付消息監聽器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentListener {

    private final OrderService orderService;

    /**
     * 監聽支付成功消息
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_PAID)
    public void handlePaymentSuccess(Map<String, Object> message) {
        log.info("【MQ】接收到支付成功消息: {}", message);

        String orderSn = message.get("orderSn") != null ? String.valueOf(message.get("orderSn")) : null;
        if (orderSn == null) {
            log.error("【MQ】支付成功消息缺少 orderSn，丟棄該消息");
            return; // 缺少關鍵參數，重試也沒用，直接返回（ACK）
        }

        try {
            orderService.handlePaymentSuccess(orderSn);
        } catch (Exception e) {
            log.error("【MQ】處理支付成功消息失敗，觸發 MQ 重試: orderSn={}, err={}", orderSn, e.getMessage());
            // 拋出例外觸發 RabbitMQ 重試機制（需配合配置中的重試策略）
            throw e;
        }
    }
}
