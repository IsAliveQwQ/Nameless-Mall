package com.nameless.mall.search.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置類 - 包含可靠性死信架構
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_ORDER_CREATED = "order.created";

    // 主佇列
    public static final String QUEUE_PRODUCT_SYNC = "product.sync.queue";
    // 死信佇列
    public static final String QUEUE_PRODUCT_SYNC_DLQ = "product.sync.dlq";

    // 主交換機 (Topic)
    public static final String EXCHANGE_PRODUCT_TOPIC = "product.topic";
    // 死信交換機 (Direct)
    public static final String EXCHANGE_PRODUCT_DLX = "product.sync.dlx";

    public static final String ROUTING_KEY_PRODUCT_SYNC = "product.sync.#";
    private static final String ROUTING_KEY_PRODUCT_DLX = "product.sync.dead";

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(QUEUE_ORDER_CREATED, true);
    }

    /**
     * 配置具備 DLX (死信交換機) 的主佇列
     */
    @Bean
    public Queue productSyncQueue() {
        Map<String, Object> args = new HashMap<>();
        // 消息失敗或過期後，投遞到此交換機
        args.put("x-dead-letter-exchange", EXCHANGE_PRODUCT_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_PRODUCT_DLX);
        return new Queue(QUEUE_PRODUCT_SYNC, true, false, false, args);
    }

    @Bean
    public Queue productSyncDLQ() {
        return new Queue(QUEUE_PRODUCT_SYNC_DLQ, true);
    }

    @Bean
    public TopicExchange productTopicExchange() {
        return new TopicExchange(EXCHANGE_PRODUCT_TOPIC);
    }

    @Bean
    public DirectExchange productDLX() {
        return new DirectExchange(EXCHANGE_PRODUCT_DLX);
    }

    @Bean
    public Binding productSyncBinding() {
        return BindingBuilder.bind(productSyncQueue())
                .to(productTopicExchange())
                .with(ROUTING_KEY_PRODUCT_SYNC);
    }

    @Bean
    public Binding productDLXBinding() {
        return BindingBuilder.bind(productSyncDLQ())
                .to(productDLX())
                .with(ROUTING_KEY_PRODUCT_DLX);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
