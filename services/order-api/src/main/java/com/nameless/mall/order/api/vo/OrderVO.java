package com.nameless.mall.order.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單基本資訊的視圖對象 (VO)
 */
@Data
public class OrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderSn;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private Integer status;
    private String statusName; // 展示狀態名稱
    private Integer payType;
    private String payTypeName;
    private Integer shippingMethod;
    private LocalDateTime createdAt;
    private Long userCouponId;
    private String failReason;
}
