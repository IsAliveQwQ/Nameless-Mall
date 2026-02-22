package com.nameless.mall.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nameless.mall.order.entity.LocalMessage;
import com.nameless.mall.order.mapper.LocalMessageMapper;
import com.nameless.mall.order.service.ReliableMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息投遞排程任務。
 * <p>
 * 定期掃描 local_message 表，發送 NEW / FAIL 狀態的消息到 RabbitMQ。
 * 使用 CAS 樂觀鎖搶佔 (status → PROCESSING)，
 * 保證多實例部署時不會重複投遞同一消息。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelayTask {

    // -- 訊息狀態常量，對應 local_message.status --
    private static final int STATUS_NEW = 0;
    private static final int STATUS_FAIL = 2;
    private static final int STATUS_PROCESSING = 9;

    private final LocalMessageMapper localMessageMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ReliableMessageService reliableMessageService;

    /**
     * 每 5 秒執行一次：回收卡住消息 → 投遞新消息 → 投遞失敗重試消息
     */
    @Scheduled(fixedDelay = 5000)
    public void relayMessages() {
        // 回收崩潰實例遺留的 PROCESSING 消息（超過 60 秒未完成）
        int recovered = localMessageMapper.recoverStaleProcessingMessages();
        if (recovered > 0) {
            log.warn("【排程】回收 {} 條卡住的 PROCESSING 消息", recovered);
        }

        processMessages(STATUS_NEW);
        processMessages(STATUS_FAIL);
    }

    private void processMessages(int targetStatus) {
        LambdaQueryWrapper<LocalMessage> query = new LambdaQueryWrapper<LocalMessage>()
                .eq(LocalMessage::getStatus, targetStatus)
                .orderByAsc(LocalMessage::getId)
                .last("LIMIT 50");

        // 失敗消息需等到退避時間到了才重試
        if (targetStatus == STATUS_FAIL) {
            query.le(LocalMessage::getNextRetryTime, LocalDateTime.now());
        }

        List<LocalMessage> messages = localMessageMapper.selectList(query);
        if (messages.isEmpty())
            return;

        log.debug("【排程】發現 {} 條{}消息，嘗試搶佔投遞",
                messages.size(), targetStatus == STATUS_NEW ? "新" : "失敗重試");

        for (LocalMessage msg : messages) {
            doSend(msg);
        }
    }

    private void doSend(LocalMessage msg) {
        try {
            // CAS 搶佔：status → PROCESSING，防止多實例重複投遞
            int claimed = localMessageMapper.update(null,
                    new LambdaUpdateWrapper<LocalMessage>()
                            .eq(LocalMessage::getId, msg.getId())
                            .eq(LocalMessage::getStatus, msg.getStatus())
                            .set(LocalMessage::getStatus, STATUS_PROCESSING)
                            .set(LocalMessage::getUpdateTime, LocalDateTime.now()));

            if (claimed == 0) {
                log.debug("【MQ】消息已被其他實例搶佔，跳過: msgId={}", msg.getMessageId());
                return;
            }

            rabbitTemplate.convertAndSend(msg.getExchange(), msg.getRoutingKey(), msg.getContent());

            reliableMessageService.markAsSent(msg.getMessageId());
            log.info("【MQ】消息發送成功: msgId={}, exchange={}", msg.getMessageId(), msg.getExchange());

        } catch (Exception e) {
            log.error("【MQ】消息發送異常: msgId={}", msg.getMessageId(), e);
            reliableMessageService.markAsFailed(msg);
        }
    }
}
