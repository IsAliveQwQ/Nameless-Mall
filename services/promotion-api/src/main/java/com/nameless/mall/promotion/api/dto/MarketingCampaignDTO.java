package com.nameless.mall.promotion.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行銷活動資料傳輸物件 (DTO)
 * 供服務層與控制器之間交換資料使用，隔離實體類別。
 */
@Data
public class MarketingCampaignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活動 ID
     */
    private Long id;

    /**
     * 活動代碼（唯一辨識碼）
     */
    private String code;

    /**
     * 活動名稱
     */
    private String name;

    /**
     * 活動標題 (展示用)
     */
    private String title;

    /**
     * 活動描述
     */
    private String description;

    /**
     * 開始時間
     */
    private LocalDateTime startTime;

    /**
     * 結束時間
     */
    private LocalDateTime endTime;

    /**
     * 活動狀態 (由 Service 動態計算後填寫)
     */
    private String status;

    /**
     * 顯示順序
     */
    private Integer displayOrder;

    /**
     * 關聯的分類 ID (為 null 表示全站適用)
     */
    private Long categoryId;

    /**
     * 活動類型 (例如滿減、折扣)
     */
    private Integer type;

    /**
     * 折扣率 (例如 0.85 表示 85 折)
     */
    private java.math.BigDecimal discountRate;

    /**
     * 滿減門檻金額
     */
    private java.math.BigDecimal thresholdAmount;
}
