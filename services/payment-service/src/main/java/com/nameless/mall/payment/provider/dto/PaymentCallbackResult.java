package com.nameless.mall.payment.provider.dto;

import com.nameless.mall.payment.api.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付回調處理結果
 * 
 * <p>
 * 封裝第三方回調或用戶確認操作的處理結果。
 * </p>
 * 
 */
@Data
@Builder
public class PaymentCallbackResult {

    /**
     * 處理是否成功
     */
    private boolean success;

    /**
     * 錯誤訊息 (失敗時)
     */
    private String errorMessage;

    /**
     * 支付單狀態
     */
    private PaymentStatus status;

    /**
     * 第三方交易編號
     */
    private String providerTradeNo;

    /**
     * 第三方原始回應 (JSON 格式，用於留存)
     */
    private String providerResponse;

    /**
     * 支付完成時間
     */
    private LocalDateTime paidAt;

    /**
     * 建立成功結果
     */
    public static PaymentCallbackResult success(String providerTradeNo) {
        return PaymentCallbackResult.builder()
                .success(true)
                .status(PaymentStatus.SUCCESS)
                .providerTradeNo(providerTradeNo)
                .paidAt(LocalDateTime.now())
                .build();
    }

    /**
     * 建立失敗結果
     */
    public static PaymentCallbackResult fail(String errorMessage) {
        return PaymentCallbackResult.builder()
                .success(false)
                .status(PaymentStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
