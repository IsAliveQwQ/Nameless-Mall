package com.nameless.mall.promotion.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒殺商品 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlashSaleProductVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private Long variantId;

    private String name;
    private String imageUrl;

    private BigDecimal originalPrice;
    private BigDecimal flashPrice;
    private String discountLabel;
    private String stockStatus;
}
