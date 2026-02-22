package com.nameless.mall.coupon.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nameless.mall.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 優惠券核銷 MQ Consumer。
 * <p>
 * 監聽 order.coupon.use 隊列，異步執行優惠券核銷（Transactional Outbox 下游）。
 * 冪等保障：useCoupon 內部檢查 status != 0 則跳過。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUseConsumer {

    private final CouponService couponService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queuesToDeclare = @Queue(value = "order.coupon.use", durable = "true"))
    public void handleCouponUse(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long userCouponId = node.get("userCouponId").asLong();
            String orderSn = node.get("orderSn").asText();

            log.info("【MQ】收到優惠券核銷請求: userCouponId={}, orderSn={}", userCouponId, orderSn);
            couponService.useCoupon(userCouponId, orderSn);
            log.info("【MQ】優惠券核銷成功: userCouponId={}, orderSn={}", userCouponId, orderSn);

        } catch (com.nameless.mall.core.exception.BusinessException e) {
            // 冪等防護：優惠券已使用/已退還 → 跳過，不重試
            log.warn("【MQ】優惠券核銷跳過（冪等）: message={}, reason={}", message, e.getMessage());
        } catch (Exception e) {
            log.error("【MQ】優惠券核銷失敗: message={}", message, e);
            // 拋出異常觸發 RabbitMQ 重試
            throw new AmqpException("優惠券核銷失敗", e);
        }
    }
}
