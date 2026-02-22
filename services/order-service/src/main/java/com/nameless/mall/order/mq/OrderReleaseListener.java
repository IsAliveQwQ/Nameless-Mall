package com.nameless.mall.order.mq;

import com.nameless.mall.order.api.enums.OrderStatus;
import com.nameless.mall.order.config.RabbitMQConfig;
import com.nameless.mall.order.entity.Order;
import com.nameless.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 訂單釋放監聽器 (超時自動取消)
 * 監聽死信隊列，處理 15 分鐘未支付的訂單。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_RELEASE)
public class OrderReleaseListener {

    private final OrderService orderService;

    /**
     * 接收並處理超時消息
     */
    @RabbitHandler
    public void handleOrderRelease(String orderSn) {
        log.info("【MQ】收到訂單超時偵測任務: orderSn={}", orderSn);

        try {
            // 1. 查詢訂單當前狀態
            Order order = orderService.getOrderBySn(orderSn);
            if (order == null) {
                log.warn("【MQ】訂單不存在，忽略超時任務: orderSn={}", orderSn);
                return;
            }

            // 2. 僅針對「待支付」訂單執行取消
            if (OrderStatus.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
                log.info("【MQ】訂單支付逾時，開始執行自動取消邏輯: orderSn={}", orderSn);
                // 呼叫現有的 cancelOrder 邏輯，該邏輯會處理：狀態更新、庫存退回、優惠券退回
                orderService.cancelOrderInternal(orderSn);
            } else {
                log.debug("【MQ】訂單狀態已變更 (status={})，無需自動取消: orderSn={}",
                        order.getStatus(), orderSn);
            }
        } catch (Exception e) {
            log.error("【MQ】處理訂單逾時任務發生 exception: orderSn={}", orderSn, e);
            // 拋出例外以觸發 RabbitMQ 重試機制 (Retry -> DLQ)
            throw new AmqpException("處理訂單逾時任務失敗", e);
        }
    }
}
