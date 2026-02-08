package com.nameless.mall.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {
    @Bean
    public OpenAPI userServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("使用者服務 API (User Service)")
                        .description("負責使用者註冊、查詢等核心業務。")
                        .version("v1.0.0")
                );
    }
}