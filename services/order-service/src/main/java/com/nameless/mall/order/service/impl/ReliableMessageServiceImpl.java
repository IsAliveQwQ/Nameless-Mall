package com.nameless.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.order.config.RabbitMQConfig;
import com.nameless.mall.order.entity.LocalMessage;
import com.nameless.mall.order.mapper.LocalMessageMapper;
import com.nameless.mall.order.service.ReliableMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 可靠訊息服務實作。
 * <p>
 * 所有 create*Message 方法都在交易內寫入 local_message 表（TX Outbox 模式），
 * 由 MessageRelayTask 輪詢後投遞到 RabbitMQ。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReliableMessageServiceImpl implements ReliableMessageService {

    // 訊息狀態常量，對應 local_message.status
    private static final int STATUS_NEW = 0;
    private static final int STATUS_SENT = 1;
    private static final int STATUS_FAIL = 2;
    private static final int STATUS_DEAD = 3;
    private static final int STATUS_PROCESSING = 9;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LocalMessageMapper localMessageMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createOrderCancelledMessage(String orderSn) {
        LocalMessage msg = buildMessage(
                orderSn,
                RabbitMQConfig.ORDER_EVENT_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY);
        localMessageMapper.insert(msg);
        log.info("【本地訊息】訂單取消消息已寫入 TX Outbox，等待投遞: orderSn={}, msgId={}", orderSn, msg.getMessageId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markAsSent(String messageId) {
        // CAS：從 PROCESSING / NEW / FAIL → SENT
        localMessageMapper.update(null,
                new LambdaUpdateWrapper<LocalMessage>()
                        .eq(LocalMessage::getMessageId, messageId)
                        .in(LocalMessage::getStatus, STATUS_NEW, STATUS_FAIL, STATUS_PROCESSING)
                        .set(LocalMessage::getStatus, STATUS_SENT)
                        .set(LocalMessage::getUpdateTime, LocalDateTime.now()));
        log.debug("【本地訊息】標記消息為已發送: msgId={}", messageId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markAsFailed(LocalMessage msg) {
        // 1. 遞增重試次數，達上限則標記為 DEAD
        int nextRetryCount = msg.getRetryCount() + 1;
        int status = (nextRetryCount >= msg.getMaxRetry()) ? STATUS_DEAD : STATUS_FAIL;

        // 2. 計算指數退避延遲時間 (5s, 10s, 20s...)
        long delaySeconds = 5L * (long) Math.pow(2, msg.getRetryCount());
        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(delaySeconds);

        // 3. CAS 更新訊息狀態與下次重試時間
        localMessageMapper.update(null,
                new LambdaUpdateWrapper<LocalMessage>()
                        .eq(LocalMessage::getId, msg.getId())
                        .in(LocalMessage::getStatus, STATUS_NEW, STATUS_FAIL, STATUS_PROCESSING)
                        .set(LocalMessage::getRetryCount, nextRetryCount)
                        .set(LocalMessage::getStatus, status)
                        .set(LocalMessage::getNextRetryTime, nextRetry)
                        .set(LocalMessage::getUpdateTime, LocalDateTime.now()));

        log.warn("【本地訊息】發送失敗，已排程重試: msgId={}, retryCount={}, nextRetry={}",
                msg.getMessageId(), nextRetryCount, nextRetry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createCouponUseMessage(Long userCouponId, String orderSn) {
        // 1. 組裝訊息 payload（優惠券 ID + 訂單編號）
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userCouponId", userCouponId);
        payload.put("orderSn", orderSn);

        // 2. 建立本地訊息並序列化 payload
        LocalMessage msg = buildMessage(
                toJson(payload, "優惠券核銷消息序列化失敗"),
                RabbitMQConfig.ORDER_EVENT_EXCHANGE,
                RabbitMQConfig.ORDER_COUPON_USE_ROUTING_KEY);
        // 3. 寫入 local_message 表（TX Outbox）
        localMessageMapper.insert(msg);
        log.info("【本地訊息】優惠券核銷消息已寫入 TX Outbox: orderSn={}, userCouponId={}, msgId={}",
                orderSn, userCouponId, msg.getMessageId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void killPendingCouponMessage(String orderSn) {
        // 透過 LIKE 模糊匹配 JSON content 中的 orderSn，將尚未發送的優惠券核銷訊息標記為 DEAD
        int rows = localMessageMapper.update(null,
                new LambdaUpdateWrapper<LocalMessage>()
                        .eq(LocalMessage::getRoutingKey, RabbitMQConfig.ORDER_COUPON_USE_ROUTING_KEY)
                        .like(LocalMessage::getContent, orderSn)
                        .in(LocalMessage::getStatus, STATUS_NEW, STATUS_FAIL)
                        .set(LocalMessage::getStatus, STATUS_DEAD)
                        .set(LocalMessage::getUpdateTime, LocalDateTime.now()));
        if (rows > 0) {
            log.info("【本地訊息】攔截未發送的優惠券核銷消息: orderSn={}, killed={}", orderSn, rows);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createOrderCreatedMessage(Long orderId, List<Long> productIds) {
        // 1. 組裝訊息 payload（訂單 ID + 商品 ID 列表）
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("productIds", productIds);

        // 2. 建立本地訊息（使用 Default Exchange，Routing Key 對應 Queue Name）
        LocalMessage msg = buildMessage(
                toJson(payload, "訂單建立消息序列化失敗"),
                "",
                RabbitMQConfig.QUEUE_ORDER_CREATED);
        // 3. 寫入 local_message 表（TX Outbox）
        localMessageMapper.insert(msg);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createOrderDelayMessage(String orderSn) {
        LocalMessage msg = buildMessage(
                orderSn,
                RabbitMQConfig.ORDER_EVENT_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY);
        localMessageMapper.insert(msg);
        log.info("【本地訊息】訂單延遲取消任務已寫入 TX Outbox: orderSn={}", orderSn);
    }

    // 私有輔助方法

    /** 建立一筆待投遞的本地訊息（共用模板） */
    private LocalMessage buildMessage(String content, String exchange, String routingKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalMessage msg = new LocalMessage();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setContent(content);
        msg.setExchange(exchange);
        msg.setRoutingKey(routingKey);
        msg.setStatus(STATUS_NEW);
        msg.setRetryCount(0);
        msg.setMaxRetry(3);
        msg.setNextRetryTime(now);
        msg.setCreateTime(now);
        msg.setUpdateTime(now);
        return msg;
    }

    /** 將物件序列化為 JSON 字串，失敗時拋出 BusinessException */
    private String toJson(Object payload, String errorMsg) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, errorMsg + ": " + e.getMessage());
        }
    }
}
