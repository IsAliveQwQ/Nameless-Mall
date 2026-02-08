package com.nameless.mall.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 支付服務啟動類
 */
@SpringBootApplication(scanBasePackages = { "com.nameless.mall" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = { "com.nameless.mall.order.api.feign", "com.nameless.mall.payment" })
@MapperScan("com.nameless.mall.payment.mapper")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
