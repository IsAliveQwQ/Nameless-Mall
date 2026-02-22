package com.nameless.mall.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 認證資訊透傳與安全過濾器。
 * <p>
 * 攔截外部對內部私有 API (/internal/**) 的存取，並清除惡意偽造的 {@code X-User-Id} Header。
 * 成功驗證 JWT Token 後，會將其 sub (User ID) 擷取並注入 Request Header 中，
 * 供下游微服務直接讀取，實現 Zero Trust 架構並降低下游解碼負擔。
 * </p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    /** 禁止外部存取的內部路徑模式 */
    private static final String INTERNAL_PATH_PATTERN = "/internal/";

    /** 用於傳遞用戶 ID 的 Header 名稱 */
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 執行安全檢查與 Header 重新封裝。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 嚴禁外部通過網關訪問內部私有介面
        if (path.contains(INTERNAL_PATH_PATTERN)) {
            log.error("【安全攔截】攔截到非法的內部路徑訪問嘗試: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 2. 清理所有外部嘗試注入的偽造 Header，確保 X-User-Id 僅由網關信任注入
        final ServerWebExchange cleanExchange = exchange.mutate()
                .request(r -> r.headers(h -> h.remove(USER_ID_HEADER)))
                .build();

        // 3. 提取 JWT 並注入正確的 User ID
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .map(auth -> (JwtAuthenticationToken) auth)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> {
                    String userId = jwt.getSubject();
                    if (userId != null && !userId.isEmpty()) {
                        log.debug("AuthFilter: Injecting verified X-User-Id: {}", userId);
                        return cleanExchange.mutate()
                                .request(r -> r.header(USER_ID_HEADER, userId))
                                .build();
                    }
                    return cleanExchange;
                })
                .defaultIfEmpty(cleanExchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
