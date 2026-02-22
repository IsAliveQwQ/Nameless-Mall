package com.nameless.mall.coupon.api.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 優惠券展示卡片 VO (Frontend Optimized)
 * <p>
 * 專為前端列表展示設計，包含狀態文本與進度條，
 * 隱藏後端複雜的 TYPE 判斷邏輯。
 */
@Data
@Builder
public class CouponCardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // == 基礎資訊 ==
    private Long id;
    private Long templateId;
    private String name;
    private String description;

    // == 展示用金額 ==
    /** 折扣顯示 (例如: "9折" 或 "$100") */
    private String amountDisplay;

    /** 門檻顯示 (例如: "滿 $1000 可用" 或 "無門檻") */
    private String thresholdDisplay;

    // == 狀態指示 ==
    /** 狀態文本 (例如: "立即領取", "已領取", "已搶光", "已過期") */
    private String statusText;

    /** 按鈕是否可點擊 (已領取或已搶光則為 false) */
    private Boolean isClaimable;

    /** 搶購進度 (0-100)，用於顯示紅色進度條 */
    private Integer progress;

    // == 有效期 ==
    /** 有效期描述 (例如: "2024.01.01 - 2024.02.01" 或 "領取後 7 天內有效") */
    private String validityPeriod;

    /** 絕對過期時間 (若為固定日期券) */
    private LocalDateTime endTime;
}
