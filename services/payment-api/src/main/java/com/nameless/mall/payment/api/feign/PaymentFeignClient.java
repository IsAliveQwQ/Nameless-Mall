package com.nameless.mall.payment.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.payment.api.dto.PaymentCreateDTO;
import com.nameless.mall.payment.api.dto.PaymentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 支付服務 Feign 客戶端
 * 供 order-service 等其他服務調用
 */
@FeignClient(name = "payment-service", path = "/payments", fallbackFactory = PaymentFeignFallback.class)
public interface PaymentFeignClient {

    /**
     * 創建支付單
     */
    @PostMapping("/internal/create")
    Result<PaymentDTO> createPayment(@RequestBody PaymentCreateDTO dto);

    /**
     * 根據訂單編號查詢支付狀態
     */
    @GetMapping("/internal/status/{orderSn}")
    Result<PaymentDTO> getPaymentByOrderSn(@PathVariable("orderSn") String orderSn);

    /**
     * 取消支付單（訂單取消時調用）
     */
    @PostMapping("/internal/cancel/{orderSn}")
    Result<Void> cancelPayment(@PathVariable("orderSn") String orderSn);
}
