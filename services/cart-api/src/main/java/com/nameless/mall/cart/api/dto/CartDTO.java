package com.nameless.mall.cart.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 完整的購物車資料傳輸物件 (DTO)
 * <p>
 * 用於從後端回傳給前端，代表特定使用者的整個購物車內容。
 */
@Data
public class CartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 購物車中的所有商品項目列表
     */
    private List<CartItemDTO> items;

    /**
     * 購物車中所有商品的總數量
     */
    private Integer totalQuantity;

    /**
     * 購物車中所有商品的總金額
     */
    private BigDecimal totalPrice;
}
