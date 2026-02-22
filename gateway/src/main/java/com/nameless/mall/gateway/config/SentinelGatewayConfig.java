package com.nameless.mall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel 網關層級配置類
 * 
 * <p>
 * 當觸發限流或熔斷時，回傳 JSON 格式而非 HTML。
 * </p>
 */
@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void init() {
        // 自定義限流/熔斷回應
        BlockRequestHandler blockRequestHandler = (serverWebExchange, throwable) -> {
            Map<String, Object> map = new HashMap<>();

            // 這裡遵循後端 Result<T> 標準結構
            map.put("code", "SERVICE_UNAVAILABLE");
            map.put("message", "系統繁忙，請稍後再試");
            map.put("data", null);

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(map));
        };

        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
