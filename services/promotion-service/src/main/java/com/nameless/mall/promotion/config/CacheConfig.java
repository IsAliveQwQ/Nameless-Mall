package com.nameless.mall.promotion.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/** Caffeine 本地快取配置 */
@Configuration
public class CacheConfig {

    public static final String CACHE_CATEGORY_TREE = "categoryTree";
    public static final String CACHE_FLASH_SALE_SESSIONS = "flashSaleSessions";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 預設配置：寫入 5 分鐘後過期，最大 1000 筆
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats());

        // 這裡可以針對不同 Cache Name 做細粒度配置，但為保持簡單，先共用預設配置
        // 若未來有需求，可擴充 registerCustomCache

        return cacheManager;
    }
}
