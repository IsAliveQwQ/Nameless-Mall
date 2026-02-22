package com.nameless.mall.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置類
 * <p>
 * 主佇列消費失敗 3 次後進入 Dead Letter Queue，避免無限重試卡死消費者。
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 支付成功消息佇列
     */
    public static final String QUEUE_PAYMENT_PAID = "payment.paid";

    /**
     * 死信交換器 / 佇列
     */
    public static final String DLX_EXCHANGE = "payment.dlx";
    public static final String DLQ_QUEUE = "payment.paid.dlq";
    public static final String DLQ_ROUTING_KEY = "payment.paid.dead";

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue paymentPaidQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_PAID)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 使用 Jackson 序列化消息，確保能正確發送 Map 或 DTO
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
