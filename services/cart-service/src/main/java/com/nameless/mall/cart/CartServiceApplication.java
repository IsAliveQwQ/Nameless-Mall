package com.nameless.mall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 購物車服務主啟動類 */
@SpringBootApplication(scanBasePackages = {
        "com.nameless.mall.cart",
        "com.nameless.mall.core"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall")
public class CartServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
