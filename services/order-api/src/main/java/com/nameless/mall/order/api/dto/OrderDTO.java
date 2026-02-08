package com.nameless.mall.order.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單基本資訊的資料傳輸物件 (DTO)
 * <p>
 * 用於訂單列表與提交訂單回應，不暴露資料庫 Entity 結構。
 */
@Data
public class OrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 訂單 ID
     */
    private Long id;

    /**
     * 訂單編號
     */
    private String orderSn;

    /**
     * 訂單總金額
     */
    private BigDecimal totalAmount;

    /**
     * 實際支付金額
     */
    private BigDecimal payAmount;

    /**
     * 優惠金額
     */
    private BigDecimal discountAmount;

    /**
     * 運費
     */
    private BigDecimal shippingFee;

    /**
     * 訂單狀態 (0:待付款; 1:處理中; 2:已出貨; 3:已完成; 4:已取消)
     */
    private Integer status;

    /**
     * 支付方式 (1:銀行轉帳, 2:貨到付款)
     */
    private Integer payType;

    /**
     * 配送方式
     */
    private Integer shippingMethod;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 使用的優惠券 ID (可選)
     */
    private Long userCouponId;
}
