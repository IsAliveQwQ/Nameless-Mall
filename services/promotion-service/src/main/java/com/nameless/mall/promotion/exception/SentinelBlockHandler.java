package com.nameless.mall.promotion.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.nameless.mall.core.domain.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 全域阻斷處理器
 * 
 * <p>
 * 用於處理 @SentinelResource 觸發的限流或熔斷。
 * </p>
 */
@Slf4j
public class SentinelBlockHandler {

    /**
     * 預設限流降級處理
     */
    public static Result<?> handleBlock(BlockException ex) {
        log.warn("Sentinel 阻斷觸發: {}", ex.getClass().getSimpleName());
        return Result.fail("SERVICE_UNAVAILABLE", "活動太熱烈了，請稍後再試 (Protected by Sentinel)");
    }
}
