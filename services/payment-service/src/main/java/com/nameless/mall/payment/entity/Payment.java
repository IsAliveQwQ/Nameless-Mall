package com.nameless.mall.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付單實體類
 */
@Data
@TableName("payments")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付單唯一 ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付單業務編號 (唯一)
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
     * 支付方式 (1=銀行轉帳, 2=貨到付款)
     */
    private Integer payMethod;

    /**
     * 支付狀態 (0=待支付, 1=已支付, 2=已退款, 3=已取消)
     */
    private Integer status;

    /**
     * 銀行代碼
     */
    private String bankCode;

    /**
     * 銀行名稱
     */
    private String bankName;

    /**
     * 匯款人姓名
     */
    private String payerName;

    /**
     * 支付帳戶資訊（銀行轉帳後五碼）
     */
    private String accountInfo;

    /**
     * 支付時間
     */
    private LocalDateTime paidAt;

    /**
     * 退款金額
     */
    private BigDecimal refundAmount;

    /**
     * 退款時間
     */
    private LocalDateTime refundAt;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 創建時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;

    /**
     * 金流提供商 (MANUAL, ECPAY, LINEPAY)
     */
    private String providerName;

    /**
     * 第三方交易編號
     */
    private String providerTradeNo;

    /**
     * 第三方回應原文 (JSON)
     */
    private String providerResponse;

    /**
     * 支付有效期
     */
    private LocalDateTime expiredAt;
}
