package com.nameless.mall.coupon.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 優惠券試算結果
 */
@Data
public class CouponCalculationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 計算出的折扣金額
     * (若是免運券，此金額應等於傳入的 shippingFee)
     */
    private BigDecimal discountAmount;

    /**
     * 折後最終金額 (商品總額 - 折扣)
     * 注意：不包含運費的加減，純粹是商品部分的折後。
     * 或是定義為：訂單應付總額？
     * 
     * 為了保持職責單一，這裡定義為「扣除優惠後的商品總額」。
     * 若小於 0 會回傳 0。
     */
    private BigDecimal finalAmount;

    /**
     * 優惠券名稱 (用於訂單快照)
     */
    private String couponName;

    /**
     * 優惠券類型 (方便訂單判斷是否為免運)
     */
    private Integer couponType;
}
