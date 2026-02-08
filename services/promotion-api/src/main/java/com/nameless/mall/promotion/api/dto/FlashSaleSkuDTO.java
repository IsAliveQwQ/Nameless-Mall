package com.nameless.mall.promotion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 限時特賣商品 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlashSaleSkuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long promotionId;
    private Long productId;
    private Long variantId;
    private String productName;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal flashSalePrice;
    private Integer flashSaleStock;
    private Integer soldCount;
    private Integer limitPerUser;
}
