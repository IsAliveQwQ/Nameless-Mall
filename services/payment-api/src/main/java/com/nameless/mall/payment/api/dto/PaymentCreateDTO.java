package com.nameless.mall.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 創建支付單 DTO
 */
@Data
public class PaymentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 關聯的訂單編號
     */
    @NotBlank(message = "訂單編號不能為空")
    private String orderSn;

    /**
     * 支付金額
     */
    private BigDecimal amount;

    /**
     * 支付方式 (1=銀行轉帳, 2=貨到付款)
     */
    @com.fasterxml.jackson.annotation.JsonProperty("payType")
    private Integer payMethod;

    /**
     * 新版支付方式 (Enum)
     */
    private com.nameless.mall.payment.api.enums.PaymentMethod method;

    /**
     * 支付帳戶資訊（銀行轉帳時填寫後五碼）
     */
    private String accountInfo;

    /** 銀行代碼 */
    private String bankCode;

    /** 銀行名稱 */
    private String bankName;

    /** 匯款人姓名 */
    private String payerName;
}
