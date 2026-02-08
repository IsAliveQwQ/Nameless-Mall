package com.nameless.mall.promotion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 活動服務啟動類
 */
@SpringBootApplication(scanBasePackages = { "com.nameless.mall" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall")
@MapperScan("com.nameless.mall.promotion.mapper")
@EnableCaching
@EnableScheduling
public class PromotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
