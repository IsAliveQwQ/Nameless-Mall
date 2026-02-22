package com.nameless.mall.coupon.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可用優惠券 VO (前端顯示用)
 */
@Data
public class ApplicableCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private String couponName;
    private Integer type; // 1=滿減, 2=折扣, 3=免運
    private BigDecimal threshold;
    private BigDecimal value; // 面額或折扣率
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * 是否可用
     */
    private Boolean usable;

    /**
     * 不可用原因 (若 usable=false)
     */
    private String reason;

    /**
     * 預計可折抵金額 (若 usable=true)
     * 讓前端實現 Zero-Latency 預覽
     */
    private BigDecimal estimatedDiscount;
}
