package com.nameless.mall.promotion.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行銷活動 View Object
 * 確保 API 回傳結構穩定，與資料庫實體隔離
 */
@Data
public class MarketingCampaignVO implements Serializable {

    private Long id;
    private String title;
    private String description;
    private String period;
    private String code;
    private String status;
    private String imageUrl;
    private Integer displayOrder;

    /** 關聯分類 ID (用於前端導購) */
    private Long categoryId;

    /** 折扣率 (例如 0.85) */
    private BigDecimal discountRate;
}
