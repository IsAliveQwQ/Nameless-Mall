package com.nameless.mall.payment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign 請求攔截器配置。
 * 自動將當前請求的 JWT Token 透傳至下游微服務。
 */
@Configuration
public class FeignClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FeignClientConfiguration.class);

    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    String token = jwt.getTokenValue();
                    log.debug("Feign RequestInterceptor: Extracting JWT for internal call. Token length: {}",
                            token.length());
                    template.header("Authorization", "Bearer " + token);
                } else {
                    log.debug("Feign RequestInterceptor: No JWT found in SecurityContext for current request.");
                }
            }
        };
    }
}
