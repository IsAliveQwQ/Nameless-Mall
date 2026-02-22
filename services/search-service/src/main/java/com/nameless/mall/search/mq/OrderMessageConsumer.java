package com.nameless.mall.search.mq;

import com.nameless.mall.search.config.RabbitMQConfig;
import com.nameless.mall.search.entity.ProductSearch;
import com.nameless.mall.search.repository.ProductSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 訂單訊息消費者
 * 接收訂單建立事件，更新 Elasticsearch 中的商品銷量
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final ProductSearchRepository productSearchRepository;

    /**
     * 監聽訂單建立訊息
     * 
     * @param message 訊息內容，包含 orderId 和 productIds
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void onOrderCreated(Map<String, Object> message) {
        try {
            // 1. 解析訂單 ID 與關聯商品 ID 列表
            Long orderId = ((Number) message.get("orderId")).longValue();
            @SuppressWarnings("unchecked")
            List<Number> productIdNumbers = (List<Number>) message.get("productIds");

            log.info("【MQ】收到訂單建立訊息，orderId: {}, productIds: {}", orderId, productIdNumbers);

            if (productIdNumbers == null || productIdNumbers.isEmpty()) {
                return;
            }

            // 2. 逐筆更新商品在 ES 中的銷量計數
            for (Number productIdNum : productIdNumbers) {
                Long productId = productIdNum.longValue();
                Optional<ProductSearch> productOpt = productSearchRepository.findById(productId);

                if (productOpt.isPresent()) {
                    ProductSearch product = productOpt.get();
                    // 銷量 +1（簡化處理，實際應根據訂單數量增加）
                    Integer currentSales = product.getSalesCount() != null ? product.getSalesCount() : 0;
                    product.setSalesCount(currentSales + 1);
                    productSearchRepository.save(product);

                    log.info("【MQ】已更新商品銷量，productId: {}, newSalesCount: {}", productId, product.getSalesCount());
                } else {
                    log.warn("【MQ】商品不存在於 ES 索引，productId: {}", productId);
                }
            }
        } catch (Exception e) {
            log.error("【MQ】處理訂單建立訊息失敗: {}", message, e);
            throw new AmqpException("更新 ES 銷量失敗", e);
        }
    }
}
