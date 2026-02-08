package com.nameless.mall.order.api.enums;

import com.nameless.mall.core.enums.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 訂單狀態枚舉
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus implements BaseEnum {
    PENDING_PAYMENT(0, "待付款"),
    PROCESSING(1, "已付款"),
    SHIPPED(2, "已出貨"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    CREATING(5, "建立中"),
    CREATE_FAILED(6, "建立失敗");

    private final Integer code;
    private final String description;
}
