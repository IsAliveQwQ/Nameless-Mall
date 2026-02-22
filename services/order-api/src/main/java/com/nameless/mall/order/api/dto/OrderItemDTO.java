package com.nameless.mall.order.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 訂單項目的資料傳輸物件 (DTO)
 * <p>
 * 用於在訂單詳情中，展示單一購買商品的快照資訊。
 */
@Data
public class OrderItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID (SPU ID)
     */
    private Long productId;

    /**
     * 商品規格ID (SKU ID)
     */
    private Long variantId;

    /**
     * 商品名稱 (交易快照)
     */
    private String productName;

    /**
     * 商品圖片 (交易快照)
     */
    private String productImage;

    /**
     * 商品單價 (交易快照)
     */
    private BigDecimal productPrice;

    /**
     * 規格名稱 (交易快照, 如: 黑色 128G)
     */
    private String skuName;

    /**
     * 購買數量
     */
    private Integer quantity;

    /**
     * 商品原價 (交易快照)
     */
    private BigDecimal originalPrice;

    /**
     * 促銷活動名稱 (交易快照)
     */
    private String promotionName;

    /**
     * 促銷折扣金額 (單件)
     */
    private BigDecimal promotionAmount;
}
