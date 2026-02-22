package com.nameless.mall.order.mq;

import com.nameless.mall.order.api.dto.FlashSaleMessage;
import com.nameless.mall.order.config.FlashSaleQueueConfig;
import com.nameless.mall.order.constant.FlashSaleConstants;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 特賣活動死信隊列消費者
 * <p>
 * 職責：
 * 1. 回補 Redis 庫存
 * 2. 更新 Redis 訂單狀態為 FAILED
 * 3. 記錄異常日誌供人工排查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleDLQListener {

    private final StringRedisTemplate redisTemplate;

    /**
     * 監聽特賣死信隊列，處理消費失敗的訊息
     */
    @RabbitListener(queues = FlashSaleQueueConfig.DLQ_FLASH_SALE)
    public void handleDeadLetter(FlashSaleMessage deadMessage, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.warn("【特賣 DLQ】收到死信訊息: userId={}, skuId={}, promotionId={}",
                    deadMessage.getUserId(), deadMessage.getSkuId(), deadMessage.getPromotionId());

            // 1. 更新 Redis 訂單狀態為 FAILED (讓前端知道搶購失敗)
            String orderStatusKey = FlashSaleConstants.CACHE_ORDER_PREFIX
                    + deadMessage.getUserId() + ":" + deadMessage.getSkuId();
            redisTemplate.opsForValue().set(orderStatusKey, FlashSaleConstants.STATUS_FAILED);

            // 2. 回補 Redis 庫存
            String stockKey = FlashSaleConstants.CACHE_STOCK_PREFIX
                    + deadMessage.getPromotionId() + ":" + deadMessage.getSkuId();
            redisTemplate.opsForValue().increment(stockKey, deadMessage.getQuantity());
            log.info("【特賣 DLQ】已回補 Redis 庫存: key={}, quantity={}", stockKey, deadMessage.getQuantity());

            // 3. 確認消息 (處理完畢)
            channel.basicAck(deliveryTag, false);
            log.info("【特賣 DLQ】死信處理完成: userId={}, skuId={}", deadMessage.getUserId(), deadMessage.getSkuId());

        } catch (Exception e) {
            log.error("【特賣 DLQ】嚴重錯誤 - 死信處理失敗! 需人工介入: msg={}", deadMessage, e);
            // DLQ 處理失敗不再重試，直接 Ack 避免死迴圈，但記錄 CRITICAL 日誌
            channel.basicAck(deliveryTag, false);
        }
    }
}
