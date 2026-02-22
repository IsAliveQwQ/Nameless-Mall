package com.nameless.mall.promotion.scheduler;

import com.nameless.mall.promotion.service.FlashSalePromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 特賣庫存自動校驗排程器。
 * <p>
 * 每 5 分鐘偵測 Redis 中的特賣庫存 key 是否遺失，
 * 若發現缺失則自動從 DB 補回，確保 Redis 與 DB 最終一致。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleStockScheduler {

    private final StringRedisTemplate redisTemplate;
    private final FlashSalePromotionService flashSalePromotionService;

    /**
     * 定時偵測 + 自動補回 Redis 庫存。
     * 每 5 分鐘執行一次，用 SCAN 檢查是否有 flash_sale:stock:* key 存在。
     * 若完全不存在，觸發全量同步。
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void verifyAndRecoverStock() {
        try {
            // 用 SCAN 取代 KEYS，避免阻塞 Redis 主線程
            boolean hasStockKey = Boolean.TRUE.equals(
                    redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                        var cursor = connection.keyCommands().scan(
                                ScanOptions.scanOptions()
                                        .match("flash_sale:stock:*")
                                        .count(1)
                                        .build());
                        boolean found = cursor.hasNext();
                        cursor.close();
                        return found;
                    }));

            if (!hasStockKey) {
                log.warn("【庫存校驗】偵測到 Redis 特賣庫存 key 全部遺失，觸發自動補回...");
                flashSalePromotionService.syncPromotionStock();
                log.info("【庫存校驗】自動補回完成。");
            } else {
                log.debug("【庫存校驗】Redis 特賣庫存正常。");
            }
        } catch (Exception e) {
            log.error("【庫存校驗】執行失敗", e);
        }
    }
}
