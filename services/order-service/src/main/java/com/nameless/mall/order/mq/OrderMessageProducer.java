package com.nameless.mall.order.mq;

import com.nameless.mall.order.service.ReliableMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 訂單消息生產者
 * 負責透過 Outbox 模式發送訂單事件消息。
 */
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageProducer.class);

    private final ReliableMessageService reliableMessageService;

    /**
     * 發送訂單取消事件 (供下游服務如 Promotion Service 釋放資源)
     * 使用本地訊息表模式，確保消息可靠落庫。
     * 後續由 MessageRelayTask 異步發送。
     */
    public void sendOrderCancelled(String orderSn) {
        log.info("【MQ】準備發送訂單取消事件: orderSn={}", orderSn);
        reliableMessageService.createOrderCancelledMessage(orderSn);
    }
}
