package com.nameless.mall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置類 - 包含訂單超時自動取消架構
 */
@Configuration
public class RabbitMQConfig {

    // 隊列名稱
    public static final String QUEUE_ORDER_CREATED = "order.created";
    public static final String QUEUE_PAYMENT_PAID = "payment.paid";

    // Payment Dead Letter (與 payment-service 側一致)
    public static final String PAYMENT_DLX_EXCHANGE = "payment.dlx";
    public static final String PAYMENT_DLQ_ROUTING_KEY = "payment.paid.dead";

    // 訂單延遲隊列 (死信來源)
    public static final String QUEUE_ORDER_DELAY = "order.delay.queue";
    // 訂單釋放隊列 (死信目的地/實際執行取消處)
    public static final String QUEUE_ORDER_RELEASE = "order.release.queue";

    // 交換機與 Routing Key
    public static final String ORDER_EVENT_EXCHANGE = "order.event.exchange";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay.key";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled.key";
    public static final String ORDER_RELEASE_ROUTING_KEY = "order.release.key";
    public static final String ORDER_COUPON_USE_ROUTING_KEY = "order.coupon.use.key";
    public static final String QUEUE_COUPON_USE = "order.coupon.use";

    /**
     * 訂單事件交換機
     */
    @Bean
    public TopicExchange orderEventExchange() {
        return new TopicExchange(ORDER_EVENT_EXCHANGE, true, false);
    }

    /**
     * 延遲隊列：消息進入後等待過期，轉入死信隊列
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 設置死信交換機
        args.put("x-dead-letter-exchange", ORDER_EVENT_EXCHANGE);
        // 設置死信 Routing Key
        args.put("x-dead-letter-routing-key", ORDER_RELEASE_ROUTING_KEY);
        // 設置過期時間 (毫秒)，此處範例設為 15 分鐘 (900000ms)
        args.put("x-message-ttl", 900000);

        return new Queue(QUEUE_ORDER_DELAY, true, false, false, args);
    }

    /**
     * 釋放隊列：實際監聽並執行超時取消的隊列
     */
    @Bean
    public Queue orderReleaseQueue() {
        return new Queue(QUEUE_ORDER_RELEASE, true, false, false);
    }

    /**
     * 綁定延遲隊列到交換機
     */
    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderEventExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 綁定釋放隊列到交換機 (死信投遞目標)
     */
    @Bean
    public Binding orderReleaseBinding() {
        return BindingBuilder.bind(orderReleaseQueue())
                .to(orderEventExchange())
                .with(ORDER_RELEASE_ROUTING_KEY);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(QUEUE_ORDER_CREATED, true);
    }

    @Bean
    public Queue paymentPaidQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_PAID)
                .withArgument("x-dead-letter-exchange", PAYMENT_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue couponUseQueue() {
        return new Queue(QUEUE_COUPON_USE, true);
    }

    @Bean
    public Binding couponUseBinding() {
        return BindingBuilder.bind(couponUseQueue())
                .to(orderEventExchange())
                .with(ORDER_COUPON_USE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
