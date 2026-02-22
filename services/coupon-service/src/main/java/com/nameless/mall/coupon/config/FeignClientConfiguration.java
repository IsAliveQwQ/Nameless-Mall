package com.nameless.mall.coupon.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign 請求攔截器配置。
 * 自動從當前的 HttpServletRequest 取出 Authorization 或 X-User-Id 表頭並透傳至下游。
 */
@Configuration
public class FeignClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FeignClientConfiguration.class);

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String authorization = request.getHeader("Authorization");
                if (authorization != null) {
                    log.debug("Feign RequestInterceptor: Extracting Authorization header. Length: {}",
                            authorization.length());
                    template.header("Authorization", authorization);
                }

                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    log.debug("Feign RequestInterceptor: Extracting X-User-Id header: {}", userId);
                    template.header("X-User-Id", userId);
                }
            } else {
                log.debug("Feign RequestInterceptor: No Request attributes found. Likely async context.");
            }
        };
    }
}
