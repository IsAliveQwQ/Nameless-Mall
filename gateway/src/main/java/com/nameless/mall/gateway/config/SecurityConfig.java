package com.nameless.mall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 安全設定。
 * 
 * 定義哪些 endpoint 需要認證、哪些可以匿名存取。
 * 
 * 白名單策略:
 * - 零信任
 * - 所有 /api/** 路徑預設 permitAll (交給下游服務自行驗證)
 * - 其他路徑需要認證
 * 
 * JWT 驗證:
 * - 使用 oauth2ResourceServer 驗證 Bearer Token
 * - JWK Set 從 auth-service 取得
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        /**
         * 定義 Security Filter Chain。
         */
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .authorizeExchange(exchange -> exchange
                                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // 嚴格禁止外部存取內部 API (優先匹配，避免被後續 permitAll 複寫)
                                                .pathMatchers("/api/*/internal/**").denyAll()
                                                .pathMatchers("/actuator/health").permitAll()
                                                .pathMatchers(
                                                                "/api/auth/**", "/api/auth", "/auth/**",
                                                                "/api/users/**", "/api/users", "/users/**",
                                                                "/api/products/**", "/api/products", "/products/**",
                                                                "/api/search/**", "/api/search", "/search/**",
                                                                "/api/cart/**", "/api/cart", "/cart/**",
                                                                "/api/coupons/**", "/api/coupons", "/coupons/**",
                                                                "/api/promotions/**", "/api/promotions",
                                                                "/promotions/**",
                                                                "/api/payments/callback/**", "/payments/callback/**",
                                                                "/api/payments/**", "/api/payments", "/payments/**",
                                                                "/api/orders/**", "/api/orders", "/orders/**",
                                                                "/swagger-ui/index.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/webjars/**",
                                                                "/api/oauth2/authorization/**",
                                                                "/oauth2/authorization/**",
                                                                "/api/login/oauth2/code/**",
                                                                "/login/oauth2/code/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

                return http.build();
        }
}