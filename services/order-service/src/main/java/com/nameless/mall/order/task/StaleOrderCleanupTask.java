package com.nameless.mall.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nameless.mall.order.api.enums.OrderStatus;
import com.nameless.mall.order.entity.Order;
import com.nameless.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排程清理卡在 CREATING 的訂單（異步處理超時的兜底機制）。
 * 每分鐘掃描一次，超時者標記 CREATE_FAILED。庫存補償由 OrderAsyncProcessor catch 負責。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleOrderCleanupTask {

    private final OrderMapper orderMapper;

    @Value("${order.stale-order.timeout-minutes:5}")
    private int timeoutMinutes;

    @Scheduled(fixedDelay = 60000)
    public void cleanupStaleCreatingOrders() {
        // 1. 查詢超時仍停留在 CREATING 狀態的訂單
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<Order> staleOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.CREATING.getCode())
                        .lt(Order::getCreatedAt, cutoff)
                        .last("LIMIT 100"));

        if (staleOrders.isEmpty()) return;

        log.warn("【卡單清理】發現 {} 筆超時 CREATING 訂單（超過 {} 分鐘），標記為 CREATE_FAILED",
                staleOrders.size(), timeoutMinutes);

        // 2. 逐筆以 CAS 方式更新狀態為 CREATE_FAILED，防止併發衝突
        for (Order order : staleOrders) {
            boolean updated = orderMapper.update(null,
                    new LambdaUpdateWrapper<Order>()
                            .eq(Order::getId, order.getId())
                            .eq(Order::getStatus, OrderStatus.CREATING.getCode()) // CAS 防併發
                            .set(Order::getStatus, OrderStatus.CREATE_FAILED.getCode())
                            .set(Order::getFailReason, "異步處理超時 (>" + timeoutMinutes + "min)，請重新下單")
                            .set(Order::getUpdatedAt, LocalDateTime.now())) > 0;

            // 3. 記錄成功標記的訂單資訊
            if (updated) {
                log.warn("【卡單清理】已標記失敗: orderId={}, orderSn={}, createdAt={}",
                        order.getId(), order.getOrderSn(), order.getCreatedAt());
            }
        }
    }
}
