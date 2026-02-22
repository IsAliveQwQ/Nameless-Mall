package com.nameless.mall.payment.provider.dto;

import com.nameless.mall.payment.api.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付初始化結果
 * 
 * <p>
 * 封裝初始化支付後的回傳資訊，包含導向 URL 或表單參數。
 * </p>
 * 
 */
@Data
@Builder
public class PaymentInitResult {

    /**
     * 初始化是否成功
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
     * 導向類型
     */
    private RedirectType redirectType;

    /**
     * 導向 URL (GET 跳轉或 Form Action)
     */
    private String redirectUrl;

    /**
     * 表單參數 (FORM_POST 類型時使用)
     */
    private Map<String, String> formParams;

    /**
     * 支付有效期
     */
    private LocalDateTime expireAt;

    /**
     * 自動提交的 HTML 表單內容 (FORM_POST 類型時使用)
     * <p>
     * 適用於 ECPay 等需要 Form POST 提交的金流商。
     * </p>
     */
    private String formData;

    /**
     * 第三方交易編號 (部分提供商在初始化階段即回傳)
     */
    private String providerTradeNo;

    /**
     * 導向類型枚舉
     */
    public enum RedirectType {
        /** 無需跳轉 (手動確認) */
        NONE,
        /** GET 方式跳轉 */
        URL_REDIRECT,
        /** Form POST 方式提交 */
        FORM_POST
    }

    /**
     * 建立成功結果 (手動確認，無需導向)
     */
    public static PaymentInitResult successManual(LocalDateTime expireAt) {
        return PaymentInitResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .redirectType(RedirectType.NONE)
                .expireAt(expireAt)
                .build();
    }

    /**
     * 建立失敗結果
     */
    public static PaymentInitResult fail(String errorMessage) {
        return PaymentInitResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .status(PaymentStatus.FAILED)
                .redirectType(RedirectType.NONE)
                .build();
    }
}
