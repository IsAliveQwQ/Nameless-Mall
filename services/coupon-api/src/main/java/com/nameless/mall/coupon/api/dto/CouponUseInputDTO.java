package com.nameless.mall.coupon.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 優惠券核銷輸入 DTO
 */
@Data
public class CouponUseInputDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用戶優惠券 ID */
    @NotNull(message = "優惠券ID不能為空")
    private Long userCouponId;

    /** 使用時的訂單號 */
    @NotBlank(message = "訂單號不能為空")
    private String orderSn;
}
