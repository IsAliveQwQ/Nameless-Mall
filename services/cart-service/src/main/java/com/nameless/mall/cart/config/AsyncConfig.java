package com.nameless.mall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 購物車服務異步配置。
 * <p>
 * 提供 cartFeignExecutor 線程池，供 getCartDTO() 並行發射 Feign 呼叫使用。
 * 使用 CallerRunsPolicy 確保過載時降級為同步執行（不會丟失請求）。
 * </p>
 */
@Slf4j
@Configuration
public class AsyncConfig {

    @Bean("cartFeignExecutor")
    public Executor cartFeignExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("cart-feign-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("【Cart 異步配置】Feign 並行線程池: core=8, max=30, queue=200");
        return executor;
    }
}
