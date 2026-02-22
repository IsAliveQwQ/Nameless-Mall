package com.nameless.mall.coupon.api.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用戶優惠券 DTO
 */
@Data
public class UserCouponDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long templateId;

    /** 冗餘：優惠券名稱 */
    private String couponName;

    /** 冗餘：優惠券類型 1=滿減 2=折扣 3=免運 */
    private Integer type;

    /** 冗餘：使用門檻 */
    private BigDecimal threshold;

    /** 冗餘：優惠金額 */
    private BigDecimal discount;

    /** 狀態: 0=未使用 1=已使用 2=已過期 */
    private Integer status;

    /** 使用時的訂單號 */
    private String orderSn;

    /** 使用時間 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime usedAt;

    /** 過期時間 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expireTime;

    /** 領取時間 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
