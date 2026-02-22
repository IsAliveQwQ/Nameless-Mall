package com.nameless.mall.payment.api.vo;

import lombok.Data;
import java.io.Serializable;
import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付單視圖對象 (VO) - 供前端與外部服務查詢使用
 */
@Data
public class PaymentVO implements Serializable {

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

    /** 支付方式 */
    private PaymentMethod method;

    /** 支付狀態 */
    private PaymentStatus paymentStatus;

    /**
     * 支付導向 URL (第三方金流用)
     * 若 redirectType = URL_REDIRECT，前端直接跳轉此 URL
     */
    private String redirectUrl;

    /**
     * 導向類型 (URL_REDIRECT | FORM_POST | NONE)
     */
    private String redirectType;

    /**
     * 自動提交表單內容 (FORM_POST 類型時使用)
     * 會是一段完整的 HTML form
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

    /** 創建時間 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /** 金流提供商 (ECPAY, LINE_PAY, MANUAL) */
    private String providerName;

    /** 支付有效期 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;
}
