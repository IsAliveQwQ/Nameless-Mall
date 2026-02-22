package com.nameless.mall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 授權服務啟動類 */
@SpringBootApplication(scanBasePackages = "com.nameless.mall")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}