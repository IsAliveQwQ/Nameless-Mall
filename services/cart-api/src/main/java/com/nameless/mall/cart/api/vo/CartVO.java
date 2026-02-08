package com.nameless.mall.cart.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 購物車 View Object (VO)
 * 對齊前端需求，提供精確的總計金額與數量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartVO implements Serializable {

    private List<CartItemVO> items;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
}
