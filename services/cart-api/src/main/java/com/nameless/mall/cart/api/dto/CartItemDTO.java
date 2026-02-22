package com.nameless.mall.cart.api.dto;

import com.nameless.mall.product.api.dto.VariantOptionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 購物車中單一項目的資料傳輸物件 (DTO)
 * <p>
 * 這個物件會被序列化成 JSON 字串，作為 Redis Hash 中的 Value 儲存。
 * 它包含了從商品服務獲取的商品資訊快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品主檔ID (product.id)
     */
    private Long productId;

    /**
     * 商品所屬分類ID (用於行銷活動匹配)
     */
    private Long categoryId;

    /**
     * 商品名稱
     */
    private String productName;

    /**
     * 商品主圖的 URL
     */
    private String productImage;

    /**
     * 商品規格ID (variant.id)，這將作為 Redis Hash 中的 Field
     */
    private Long variantId;

    /**
     * 商品規格的 SKU 編碼
     */
    private String sku;

    /**
     * 規格的選項列表，用於在購物車頁面顯示詳細規格。
     * 例如: [ {optionName:"顏色", optionValue:"白色"}, {optionName:"容量",
     * optionValue:"256GB"} ]
     */
    private List<VariantOptionDTO> options;

    /**
     * 購買數量
     */
    private Integer quantity;

    /**
     * 加入購物車時的商品原價 (未折扣)
     */
    private BigDecimal originalPrice;

    /**
     * 活動折扣金額 (originalPrice - finalPrice)
     */
    private BigDecimal discountAmount;

    /**
     * 加入購物車時的商品單價 (最終成交價)
     */
    private BigDecimal price;

    /**
     * 促銷活動名稱 (若有)
     */
    private String promotionName;

    /**
     * 促銷活動類型 (e.g. FLASH_SALE, CAMPAIGN)
     */
    private String promotionType;

    /**
     * 促銷活動ID（用於特賣庫存扣減與記錄）
     */
    private Long promotionId;
}
