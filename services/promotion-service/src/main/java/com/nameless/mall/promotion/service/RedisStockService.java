package com.nameless.mall.promotion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Redis 庫存管理服務 - 基於 LUA 腳本實現原子扣減
 */
@Slf4j
@Service
public class RedisStockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> deductScript;

    public RedisStockService() {
        // 在建構子中加載腳本，提高執行效能
        this.deductScript = new DefaultRedisScript<>();
        this.deductScript.setLocation(new ClassPathResource("lua/stock_deduct.lua"));
        this.deductScript.setResultType(Long.class);
    }

    /**
     * 預熱庫存到 Redis
     */
    public void prepareStock(Long promotionId, Long skuId, Integer stock) {
        String key = getStockKey(promotionId, skuId);
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("【Redis 預熱】Key={}, Stock={}", key, stock);
    }

    /**
     * 原子扣減庫存
     * 
     * @return true: 扣減成功, false: 庫存不足或未預熱
     */
    public boolean deduct(Long promotionId, Long skuId, int quantity) {
        String key = getStockKey(promotionId, skuId);

        // 呼叫預加載的 LUA 腳本
        Long result = redisTemplate.execute(deductScript,
                Collections.singletonList(key),
                String.valueOf(quantity));

        if (result == null || result == -2) {
            log.warn("【Redis 扣減失敗】Key 不存在，請檢查預熱流程: {}", key);
            return false;
        }

        if (result == -1) {
            log.warn("【Redis 扣減失敗】庫存不足: {}", key);
            return false;
        }

        log.info("【Redis 扣減成功】Key={}, 剩餘庫存={}", key, result);
        return true;
    }

    /**
     * 原子返還庫存 (用於 DB 事務回滾補償)
     */
    public void recoverStock(Long promotionId, Long skuId, int quantity) {
        String key = getStockKey(promotionId, skuId);
        redisTemplate.opsForValue().increment(key, (long) quantity);
        log.info("【Redis 補償】Key={}, 返還數量={}", key, quantity);
    }

    private String getStockKey(Long promotionId, Long skuId) {
        return "flash_sale:stock:" + promotionId + ":" + skuId;
    }
}
