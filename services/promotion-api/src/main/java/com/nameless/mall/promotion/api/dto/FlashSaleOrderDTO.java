package com.nameless.mall.promotion.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 限時特賣下單請求 DTO
 */
@Data
public class FlashSaleOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 限時特賣活動 ID */
    @NotNull(message = "活動 ID 不能為空")
    private Long promotionId;

    /** 限時特賣商品 SKU ID */
    @NotNull(message = "SKU ID 不能為空")
    private Long flashSaleSkuId;

    /** 購買數量 */
    @NotNull(message = "購買數量不能為空")
    @Min(value = 1, message = "購買數量至少為 1")
    private Integer quantity;

    /** 收貨地址 ID */
    @NotNull(message = "收貨地址不能為空")
    private Long addressId;
}
