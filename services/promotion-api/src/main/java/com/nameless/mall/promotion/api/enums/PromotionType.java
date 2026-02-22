package com.nameless.mall.promotion.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 促銷類型枚舉
 */
@Getter
@RequiredArgsConstructor
public enum PromotionType {
    NONE("無優惠"),
    FLASH_SALE("限時特賣"),
    CAMPAIGN("行銷活動"),
    COUPON("優惠券"),
    DEGRADED("服務降級");

    private final String description;
}
