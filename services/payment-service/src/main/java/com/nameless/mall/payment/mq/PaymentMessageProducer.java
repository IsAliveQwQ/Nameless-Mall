package com.nameless.mall.payment.mq;

import com.nameless.mall.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付消息生產者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 發送支付成功消息
     * 
     * @param orderSn   訂單編號
     * @param paymentSn 支付單號
     */
    public void sendPaymentSuccess(String orderSn, String paymentSn) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderSn", orderSn);
        message.put("paymentSn", paymentSn);
        message.put("timestamp", System.currentTimeMillis());

        log.info("【MQ】發送支付成功消息 -> orderSn: {}, paymentSn: {}", orderSn, paymentSn);

        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_PAYMENT_PAID, message);
    }
}
