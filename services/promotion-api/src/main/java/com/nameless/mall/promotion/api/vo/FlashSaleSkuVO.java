package com.nameless.mall.promotion.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 限時特賣商品 VO
 */
@Data
public class FlashSaleSkuVO implements Serializable {

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
