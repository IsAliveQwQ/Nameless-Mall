package com.nameless.mall.core.enums;

/**
 * 通用 Enum 介面。
 * 
 * 所有需要支援 URL Query Parameter 自動轉換的 Enum 都要實作這個介面。
 * StringToBaseEnumConverterFactory 會根據 getCode() 的值進行 mapping。
 * 
 * 使用方式:
 * public enum OrderStatus implements BaseEnum {
 * PENDING(0), PAID(1), SHIPPED(2);
 * 
 * private final Integer code;
 * // constructor + getCode()...
 * }
 * 
 * @see StringToBaseEnumConverterFactory
 */
public interface BaseEnum {
    /**
     * 取得 Enum 對應的整數 code。
     * 用於 URL Query Parameter 轉換，例如 ?status=1 會對應到 PAID。
     */
    Integer getCode();
}
