package com.nameless.mall.cart.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 購物車項目 View Object (VO)
 * 對齊前端需求，隱藏內部細節，提供扁平化的規格資訊。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO implements Serializable {

    private Long productId;
    private Long variantId;
    private String productName;
    private String productImage;
    private String skuCode;
    /**
     * 規格描述資訊，例如 "顏色: 白色 / 尺寸: L"
     */
    private String specInfo;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;

    // 促銷相關欄位
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private String promotionName;
    private String promotionType;
}
