package com.nameless.mall.promotion.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 監聽訂單事件以釋放特賣庫存
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_STOCK_RELEASE = "promotion.stock.release.queue";
    public static final String EXCHANGE_ORDER_EVENT = "order.event.exchange";
    public static final String ROUTING_KEY_ORDER_CANCELLED = "order.cancelled.key";

    @Bean
    public Queue stockReleaseQueue() {
        // 設定 durable=true, exclusive=false, autoDelete=false
        return new Queue(QUEUE_STOCK_RELEASE, true);
    }

    @Bean
    public TopicExchange orderEventExchange() {
        // 需與 Order Service 中定義的 Exchange 屬性一致 (durable=true, autoDelete=false)
        return new TopicExchange(EXCHANGE_ORDER_EVENT, true, false);
    }

    @Bean
    public Binding stockReleaseBinding() {
        return BindingBuilder.bind(stockReleaseQueue())
                .to(orderEventExchange())
                .with(ROUTING_KEY_ORDER_CANCELLED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
