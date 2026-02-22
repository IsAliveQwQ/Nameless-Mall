package com.nameless.mall.payment.api.dto;

import lombok.Data;
import java.io.Serializable;
import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付單 DTO（用於查詢返回）
 */
@Data
public class PaymentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 支付單 ID */
    private Long id;

    /** 支付單業務編號 */
    private String paymentSn;

    /** 關聯訂單編號 */
    private String orderSn;

    /** 用戶 ID */
    private Long userId;

    /** 支付金額 */
    private BigDecimal amount;

    /**
     * 支付方式 (已棄用，請使用 method)
     *
     * @deprecated 使用 {@link #method}
     */
    @Deprecated
    private Integer payMethod;

    /**
     * 新版支付方式枚舉
     */
    private PaymentMethod method;

    /**
     * 支付狀態 (已棄用，請使用 paymentStatus)
     * 
     * @deprecated 使用 {@link #paymentStatus}
     */
    @Deprecated
    private Integer status;

    /**
     * 新版支付狀態枚舉
     */
    private PaymentStatus paymentStatus;

    /**
     * 支付導向 URL (第三方金流用)
     */
    private String redirectUrl;

    /**
     * 導向類型 (URL_REDIRECT | FORM_POST | NONE)
     */
    private String redirectType;

    /**
     * 自動提交表單內容 (FORM_POST 類型時使用)
     */
    private String formData;

    /** 銀行代碼 */
    private String bankCode;

    /** 銀行名稱 */
    private String bankName;

    /** 匯款人姓名 */
    private String payerName;

    /** 支付帳戶資訊 */
    private String accountInfo;

    /** 支付時間 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paidAt;

    /** 退款金額 */
    private BigDecimal refundAmount;

    /** 創建時間 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 金流提供商
     */
    private String providerName;

    /**
     * 第三方交易編號
     */
    private String providerTradeNo;

    /**
     * 支付有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;
}
