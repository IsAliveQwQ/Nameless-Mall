package com.nameless.mall.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Auth Service 安全配置。
 * <p>
 * 設計決策：
 * <ul>
 * <li>CSRF：已停用。本服務僅作為後端 API，採用 JWT 無狀態認證，無 Cookie Session。</li>
 * <li>FormLogin：已停用。登入改由 {@code /auth/login} REST 端點處理。</li>
 * <li>OAuth2 Login：啟用 Google 社交登入，成功後由 {@link CustomOAuth2LoginSuccessHandler}
 * 發放 JWT。</li>
 * <li>JWK Set 端點公開：其他微服務需要透過此端點取得公鑰來驗證 JWT。</li>
 * </ul>
 * <p>
 * 注意：Gateway 層有自己的 SecurityConfig，負責全域路由級別的認證規則。
 * 本服務的 SecurityConfig 僅管控 auth-service 自身的端點。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationSuccessHandler customOAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // JWK Set 端點 — 其他服務需透過此端點獲取公鑰驗證 JWT
                        .requestMatchers("/oauth2/jwks", "/.well-known/jwks.json").permitAll()
                        .requestMatchers(
                                "/auth/login",
                                "/oauth2/**",
                                "/login/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2.successHandler(customOAuth2LoginSuccessHandler));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}