package com.nameless.mall.product.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 商品狀態枚舉
 */
@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    private final int code;
    private final String description;
}
