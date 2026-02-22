package com.nameless.mall.payment.provider.dto;

import com.nameless.mall.payment.api.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付初始化上下文
 * 
 * <p>
 * 封裝初始化支付所需的所有資訊。
 * </p>
 * 
 */
@Data
@Builder
public class PaymentInitContext {

    /**
     * 系統支付單編號 (唯一)
     */
    private String paymentSn;

    /**
     * 關聯訂單編號
     */
    private String orderSn;

    /**
     * 用戶 ID
     */
    private Long userId;

    /**
     * 支付金額
     */
    private BigDecimal amount;

    /**
     * 幣別 (預設 TWD)
     */
    @Builder.Default
    private String currency = "TWD";

    /**
     * 付款方式
     */
    private PaymentMethod method;

    /**
     * 商品描述 (顯示於第三方頁面)
     */
    private String itemName;

    /**
     * 前端回調頁面 URL (用戶付款後導回)
     */
    private String returnUrl;

    /**
     * 後端 Server-to-Server 回調 URL
     */
    private String notifyUrl;
}
