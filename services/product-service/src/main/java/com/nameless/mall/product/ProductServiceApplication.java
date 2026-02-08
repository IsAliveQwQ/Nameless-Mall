package com.nameless.mall.product;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 商品服務啟動類
 */
@Slf4j
@MapperScan("com.nameless.mall.product.mapper")
@SpringBootApplication(scanBasePackages = {
        "com.nameless.mall.product",
        "com.nameless.mall.core"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall.product.api.feign")
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
        log.info("Nameless Mall 商品服務啟動成功。");
    }
}
