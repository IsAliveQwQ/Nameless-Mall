package com.nameless.mall.promotion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 特賣庫存扣減請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlashSaleDeductionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long promotionId;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Long userId;
    private String orderSn;
}
