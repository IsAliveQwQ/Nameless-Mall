package com.nameless.mall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/** Order-Service 主啟動類 */
@Slf4j
@SpringBootApplication(scanBasePackages = { "com.nameless.mall" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        log.info("(♥◠‿◠)ﾉﾞ  訂單服務啟動成功   ლ(´ڡ`ლ)ﾞ");
    }

}
