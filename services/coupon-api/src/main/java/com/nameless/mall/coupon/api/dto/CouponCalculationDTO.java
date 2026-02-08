package com.nameless.mall.coupon.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 優惠券試算請求 DTO
 */
@Data
public class CouponCalculationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 當前用戶 ID (安全校驗用)
     */
    private Long userId;

    /**
     * 要使用的優惠券 ID
     * (查詢可用列表時可為 null)
     */
    private Long userCouponId;

    /**
     * 訂單商品總金額 (不含運費)
     */
    private BigDecimal orderTotalAmount;

    /**
     * 運費金額 (用於計算免運券)
     */
    private BigDecimal shippingFee;

    /**
     * 訂單內的商品 ID 列表
     * (用於未來擴充：特定商品可用/不可用)
     */
    private List<Long> productIds;
}
