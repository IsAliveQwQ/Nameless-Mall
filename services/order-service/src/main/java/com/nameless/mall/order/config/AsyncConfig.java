package com.nameless.mall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 異步下單線程池配置，參數可透過 Nacos 調整。
 * <p>
 * 關鍵設計：orderAsyncExecutor 專門給 @Async 外層任務使用，
 * feignCallExecutor 專門給 CompletableFuture 子任務（Feign RPC）使用。
 * 兩池分離避免「父線程阻塞等子線程、子線程搶不到線程」的飢餓死鎖。
 * </p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${order.async.core-pool-size:8}")
    private int corePoolSize;

    @Value("${order.async.max-pool-size:30}")
    private int maxPoolSize;

    @Value("${order.async.queue-capacity:200}")
    private int queueCapacity;

    /** 外層 @Async 任務池：每筆訂單佔 1 個線程，阻塞等待子任務完成。 */
    @Bean("orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("order-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("【異步配置】訂單外層線程池: core={}, max={}, queue={}", corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }

    /** 內層 Feign RPC 子任務池：專門跑 CompletableFuture 的 Feign 呼叫，不會被父線程占滿。 */
    @Bean("feignCallExecutor")
    public Executor feignCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("feign-rpc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("【異步配置】Feign RPC 子任務池: core=16, max=50, queue=100");
        return executor;
    }
}
