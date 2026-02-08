package com.nameless.mall.promotion.enums;

import com.nameless.mall.core.enums.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 特賣活動狀態枚舉
 */
@Getter
@RequiredArgsConstructor
public enum PromotionStatus implements BaseEnum {
    DISABLED(0, "已禁用"),
    ACTIVE(1, "進行中"),
    EXPIRED(2, "已過期");

    private final Integer code;
    private final String description;
}
