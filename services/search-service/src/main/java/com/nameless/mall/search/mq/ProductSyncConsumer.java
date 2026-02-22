package com.nameless.mall.search.mq;

import com.nameless.mall.search.config.RabbitMQConfig;
import com.nameless.mall.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商品同步消息消費者
 * 監聽來自 Product Service 的商品異動事件，實時更新 ES 索引
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncConsumer {

    private final SearchService searchService;

    /**
     * 監聽商品同步佇列
     * 
     * @param message 包含 productId 和 action ("UPDATE" 或 "DELETE")
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCT_SYNC)
    public void onProductChanged(Map<String, Object> message) {
        log.info("【MQ】收到商品變更消息: {}", message);

        try {
            // 1. 解析消息中的商品 ID 與操作類型
            Long productId = ((Number) message.get("productId")).longValue();
            String action = (String) message.get("action");

            // 2. 依操作類型同步或刪除 ES 索引
            if ("UPDATE".equalsIgnoreCase(action)) {
                searchService.syncOne(productId);
            } else if ("DELETE".equalsIgnoreCase(action)) {
                searchService.deleteOne(productId);
            } else {
                log.warn("【MQ】未知商品動作類型: {}", action);
            }
        } catch (Exception e) {
            log.error("【MQ】處理商品變更消息失敗: productId={}, error={}", message.get("productId"), e.getMessage());
            // 重新拋出以觸發 RabbitMQ 重試，多次失敗後流轉至 DLQ
            throw new AmqpException("商品同步失敗，等待 MQ 重試", e);
        }
    }
}
