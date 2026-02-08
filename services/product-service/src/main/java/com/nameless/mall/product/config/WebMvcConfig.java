
package com.nameless.mall.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 阻止 Spring MVC 將 /products/internal/** 的 Feign 請求路徑視為靜態資源。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 將內部 API 路徑排除出靜態資源處理，讓 DispatcherServlet 繼續路由
                registry.addResourceHandler("/products/internal/**")
                                .addResourceLocations("");

                // 保持 Spring Boot 預設的靜態資源處理器
                registry.addResourceHandler("/**")
                                .addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/",
                                                "classpath:/static/", "classpath:/public/")
                                .setCachePeriod(0);
        }
}