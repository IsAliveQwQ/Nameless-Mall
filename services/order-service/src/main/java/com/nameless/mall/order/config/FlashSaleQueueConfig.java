package com.nameless.mall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 特賣活動專用 RabbitMQ 配置
 * <p>
 * 隔離核心訂單流，確保特賣高併發不影響一般業務。
 * 包含 Dead Letter Queue (DLQ) 機制，用於處理消費失敗的訊息。
 */
@Configuration
public class FlashSaleQueueConfig {

    // 主隊列常數
    public static final String EXCHANGE_FLASH_SALE = "order.flash.direct";
    public static final String QUEUE_FLASH_SALE_ORDER = "order.flash.queue";
    public static final String KEY_FLASH_SALE = "flash.sale";

    // 死信隊列常數
    public static final String DLX_FLASH_SALE = "order.flash.dlx";
    public static final String DLQ_FLASH_SALE = "order.flash.dlq";
    public static final String KEY_FLASH_SALE_DEAD = "flash.sale.dead";

    /**
     * 特賣專用直連交換機
     */
    @Bean
    public DirectExchange flashSaleExchange() {
        return new DirectExchange(EXCHANGE_FLASH_SALE, true, false);
    }

    /**
     * 特賣訂單佇列 (綁定死信交換機)
     * <p>
     * 消費失敗的訊息會自動轉發到 DLX
     */
    @Bean
    public Queue flashSaleOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_FLASH_SALE);
        args.put("x-dead-letter-routing-key", KEY_FLASH_SALE_DEAD);
        return new Queue(QUEUE_FLASH_SALE_ORDER, true, false, false, args);
    }

    /**
     * 綁定：將主隊列綁定到主交換機
     */
    @Bean
    public Binding flashSaleBinding() {
        return BindingBuilder.bind(flashSaleOrderQueue())
                .to(flashSaleExchange())
                .with(KEY_FLASH_SALE);
    }

    /**
     * 死信交換機
     */
    @Bean
    public DirectExchange flashSaleDLX() {
        return new DirectExchange(DLX_FLASH_SALE, true, false);
    }

    /**
     * 死信隊列
     * <p>
     * 接收消費失敗的訊息，由 FlashSaleDLQListener 統一處理回補邏輯
     */
    @Bean
    public Queue flashSaleDLQ() {
        return new Queue(DLQ_FLASH_SALE, true);
    }

    /**
     * 綁定：將死信隊列綁定到死信交換機
     */
    @Bean
    public Binding flashSaleDLQBinding() {
        return BindingBuilder.bind(flashSaleDLQ())
                .to(flashSaleDLX())
                .with(KEY_FLASH_SALE_DEAD);
    }
}
