package com.nameless.mall.order.mq;

import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.order.api.dto.FlashSaleMessage;
import com.nameless.mall.order.config.FlashSaleQueueConfig;
import com.nameless.mall.order.constant.FlashSaleConstants;
import com.nameless.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 特賣訂單 MQ 消費者：監聽特賣隊列，異步建立 DB 訂單。
 * 消費失敗自動轉入 DLQ，由 FlashSaleDLQListener 回補庫存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleOrderListener {

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 監聽特賣活動隊列，異步建立訂單
     */
    @RabbitListener(queues = FlashSaleQueueConfig.QUEUE_FLASH_SALE_ORDER)
    public void receiveFlashSaleOrder(FlashSaleMessage flashSaleMessage, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String orderStatusKey = FlashSaleConstants.CACHE_ORDER_PREFIX + flashSaleMessage.getUserId() + ":"
                + flashSaleMessage.getSkuId();

        try {
            log.info("【特賣消費者】收到下單請求: {}", flashSaleMessage);

            // 1. 冪等性檢查: 再次確認 Redis 狀態
            // 如果狀態已經是 OrderSn，代表處理過了
            String status = redisTemplate.opsForValue().get(orderStatusKey);
            if (status != null && !FlashSaleConstants.STATUS_PENDING.equals(status)
                    && !FlashSaleConstants.STATUS_FAILED.equals(status)) {
                log.info("【特賣消費者】訂單已處理過: status={}, Ack", status);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 執行建單 (DB 事務)
            String orderSn = orderService.createFlashSaleOrder(flashSaleMessage);

            // 3. 更新 Redis 狀態 (通知前端成功)
            if (orderSn != null) {
                redisTemplate.opsForValue().set(orderStatusKey, orderSn);

                // 確認消息
                channel.basicAck(deliveryTag, false);
                log.info("【特賣消費者】建單成功: orderSn={}", orderSn);
            } else {
                throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "建單返回 null");
            }

        } catch (Exception e) {
            log.error("【特賣消費者】建單失敗: msg={}, error={}", flashSaleMessage, e.getMessage(), e);

            // 失敗處理策略：
            // 拒絕消息，不重回隊列 -> 自動轉入 DLQ (由 FlashSaleDLQListener 處理回補)
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
