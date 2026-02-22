package com.nameless.mall.promotion.listener;

import com.nameless.mall.promotion.config.RabbitMQConfig;
import com.nameless.mall.promotion.service.FlashSalePromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 特賣庫存釋放監聽器
 * 監聽訂單取消事件，冪等地釋放 Redis 與 DB 庫存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_RELEASE)
public class StockReleaseListener {

    private final FlashSalePromotionService flashSalePromotionService;

    @RabbitHandler
    public void handleOrderCancelled(String orderSn) {
        try {
            log.info("【MQ】收到訂單取消事件，準備釋放特賣庫存: orderSn={}", orderSn);
            flashSalePromotionService.recoverStock(orderSn);
        } catch (Exception e) {
            log.error("【MQ】庫存釋放失敗，可能需要重試: orderSn={}, error={}", orderSn, e.getMessage(), e);
            // 拋出異常以觸發 RabbitMQ 重試機制 (預設)
            throw e;
        }
    }
}
