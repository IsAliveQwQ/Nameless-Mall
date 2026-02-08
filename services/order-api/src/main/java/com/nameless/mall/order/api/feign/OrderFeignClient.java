package com.nameless.mall.order.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 訂單服務 Feign Client
 */
@FeignClient(name = "order-service", path = "/orders", fallbackFactory = OrderFeignFallback.class)
public interface OrderFeignClient {

    /**
     * [內部介面] 透過訂單編號查詢訂單詳情 (不檢查用戶權限)
     */
    @GetMapping("/internal/{orderSn}")
    Result<OrderDetailDTO> getOrderDetail(@PathVariable("orderSn") String orderSn);
}
