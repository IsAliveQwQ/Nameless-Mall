package com.nameless.mall.promotion.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 請求計算商品最佳價格的參數
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceCheckDTO {
    /**
     * 商品 ID (用於常駐活動判斷)
     */
    private Long productId;

    /**
     * 規格 ID (用於特賣活動判斷)
     */
    private Long variantId;

    /**
     * 商品分類 ID (用於類別折扣判斷)
     */
    private Long categoryId;

    /**
     * 原始售價
     */
    private BigDecimal originalPrice;
}
