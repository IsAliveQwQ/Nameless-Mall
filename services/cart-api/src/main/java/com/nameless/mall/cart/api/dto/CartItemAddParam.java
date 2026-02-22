package com.nameless.mall.cart.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 購物車新增商品項目的請求參數 DTO。
 * 用於接收前端或外部服務傳入的新增購物車商品的資訊。
 */
@Data
public class CartItemAddParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品規格 ID (SKU ID)。
     * 這是購物車要新增的具體商品規格的唯一識別符。
     */
    @NotNull(message = "商品規格ID不能為空")
    private Long variantId;

    /**
     * 新增的數量。
     * 必須大於 0。
     */
    @NotNull(message = "數量不能為空")
    private Integer quantity;

}