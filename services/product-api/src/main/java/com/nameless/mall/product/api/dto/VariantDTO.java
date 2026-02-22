package com.nameless.mall.product.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品規格庫存資料傳輸物件 - 代表一個完整的變體
 * <p>
 * 用於在商品詳細頁中，展示一個具體、可供選擇的購買選項。
 * [更新] 同時也用於 Feign 呼叫，為購物車服務提供必要的商品快照資訊。
 */
@Data
public class VariantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 商品主檔資訊 (冗餘欄位，方便 Feign 呼叫)

    /**
     * 所屬的商品 ID (product.id)
     */
    private Long productId;

    /**
     * 商品名稱
     */
    private String productName;

    /**
     * 商品主圖 URL
     */
    private String image;

    /**
     * 商品所屬分類ID (用於行銷活動)
     */
    private Long categoryId;

    // 規格自身資訊

    /**
     * 規格的唯一ID (variants.id)
     */
    private Long id;

    /**
     * 庫存單位編碼
     */
    private String sku;

    /**
     * 規格名稱 (如: 黑色 128G)
     */
    private String name;

    /**
     * 此規格的獨立價格。
     * 如果為 null，則表示使用商品主價格。
     */
    private BigDecimal price;

    /**
     * 此規格的原價
     */
    private BigDecimal originalPrice;

    /**
     * 此規格的獨立庫存。
     */
    private Integer stock;

    /**
     * 組成此規格的所有選項列表。
     * 例如: [ {optionName:"顏色", optionValue:"白色"}, {optionName:"容量",
     * optionValue:"256GB"} ]
     */
    private List<VariantOptionDTO> options;
}
