package com.nameless.mall.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 扣減庫存輸入傳輸物件
 * <p>
 * 用於在分散式交易中，從訂單服務向商品服務傳遞需要扣減的庫存資訊。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecreaseStockInputDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品規格ID
     */
    private Long variantId;

    /**
     * 要扣減的數量
     */
    private Integer quantity; // <-- 這裡是 quantity
}
