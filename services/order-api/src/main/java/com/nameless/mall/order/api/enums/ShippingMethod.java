package com.nameless.mall.order.api.enums;

import com.nameless.mall.core.enums.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 配送方式枚舉
 */
@Getter
@RequiredArgsConstructor
public enum ShippingMethod implements BaseEnum {
    HOME_DELIVERY(1, "宅配"),
    CONVENIENCE_STORE(2, "超商取貨");

    private final Integer code;
    private final String description;
}
