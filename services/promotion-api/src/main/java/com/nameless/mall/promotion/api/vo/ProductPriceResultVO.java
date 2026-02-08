package com.nameless.mall.promotion.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品最佳價格計算結果 VO
 */
@Data
public class ProductPriceResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 規格 ID
     */
    private Long variantId;

    /**
     * 計算時使用的基準原價 (MSRP)
     */
    private BigDecimal originalPrice;

    /**
     * 最終計算後的價格 (取最低價)
     */
    private BigDecimal finalPrice;

    /**
     * 折扣節省金額 (Original - Final)
     */
    private BigDecimal discountAmount;

    /**
     * 應用的活動類型 (e.g. "FLASH_SALE", "MARKETING_CAMPAIGN", "NONE")
     */
    private String promotionType;

    /**
     * 應用的活動 ID
     */
    private Long promotionId;

    /**
     * 活動名稱 (用於前端顯示標籤)
     */
    private String promotionName;
}
