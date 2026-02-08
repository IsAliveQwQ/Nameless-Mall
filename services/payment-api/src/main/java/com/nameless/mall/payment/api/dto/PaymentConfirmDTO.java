package com.nameless.mall.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 確認支付 DTO（用於銀行轉帳確認）
 */
@Data
public class PaymentConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付單業務編號
     */
    @NotBlank(message = "支付單編號不能為空")
    private String paymentSn;

    /**
     * 支付帳戶資訊（銀行轉帳後五碼）
     */
    @NotBlank(message = "帳戶資訊不能為空")
    private String accountInfo;

    /** 銀行代碼 */
    private String bankCode;

    /** 銀行名稱 */
    private String bankName;

    /** 匯款人姓名 */
    private String payerName;
}

