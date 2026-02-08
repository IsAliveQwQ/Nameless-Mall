package com.nameless.mall.order.api.enums;

import com.nameless.mall.core.enums.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 訂單來源枚舉
 */
@Getter
@RequiredArgsConstructor
public enum OrderSource implements BaseEnum {
    PC(1, "PC 端"),
    MOBILE(2, "行動端"),
    APP(3, "APP 端");

    private final Integer code;
    private final String description;
}
