package com.nameless.mall.product.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQConfig {

    /** 商品主題交換機名稱 */
    public static final String EXCHANGE_PRODUCT_TOPIC = "product.topic";

    @Bean
    public TopicExchange productTopicExchange() {
        return new TopicExchange(EXCHANGE_PRODUCT_TOPIC, true, false);
    }

    /** 使用 Jackson JSON 序列化消息，確保跨服務相容性。 */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
