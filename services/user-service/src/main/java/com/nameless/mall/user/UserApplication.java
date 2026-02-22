package com.nameless.mall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用戶服務啟動類
 */
@MapperScan("com.nameless.mall.user.mapper")
@SpringBootApplication(scanBasePackages = { "com.nameless.mall" })
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
