package com.nameless.mall.coupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 優惠券服務啟動類
 */
@SpringBootApplication(scanBasePackages = { "com.nameless.mall" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall")
@MapperScan("com.nameless.mall.coupon.mapper")
public class CouponServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponServiceApplication.class, args);
    }
}
