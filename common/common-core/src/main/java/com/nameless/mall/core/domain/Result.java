package com.nameless.mall.core.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nameless.mall.core.enums.ResultCodeEnum;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 
 * 所有 API 回應都用這個 class 封裝，無論成功或失敗。
 * 
 * 
 * 回應格式:
 * 成功: { "code": "OK", "message": "成功", "data": {...} }
 * 失敗: { "code": "PRODUCT_NOT_FOUND", "message": "商品不存在", "data": null }
 * 
 * 使用範例:
 * - return Result.ok();
 * - return Result.ok(productDTO);
 * - return Result.fail(ResultCodeEnum.PRODUCT_NOT_FOUND);
 * - return Result.fail(ResultCodeEnum.STOCK_INSUFFICIENT, "iPhone 15 庫存不足");
 * 
 * @param <T> data 欄位的泛型類型
 * @see ResultCodeEnum
 * @see BusinessException
 */
@Getter
@ToString
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字串錯誤碼 (SCREAMING_SNAKE_CASE)。
     * <p>
     * 例如："OK", "UNAUTHORIZED", "PRODUCT_NOT_FOUND"。
     * 前端可直接使用此欄位進行錯誤判斷。
     */
    private final String code;

    /**
     * 人類可讀的訊息 (繁體中文)。
     * <p>
     * 可直接顯示給終端用戶，無需前端再轉換。
     */
    private final String message;

    /**
     * 回應資料載荷。
     * <p>
     * 成功時通常包含業務資料 (如 DTO、List 等)；失敗時通常為 null。
     */
    private final T data;

    /**
     * 【供 Jackson 反序列化使用的建構子】
     */
    @JsonCreator
    public Result(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("data") T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 【私有建構子 - 基於 ResultCodeEnum】
     */
    private Result(ResultCodeEnum resultCode, T data, String customMessage) {
        this.code = resultCode.getCode(); // 回傳 String (Enum.name())
        this.message = (customMessage == null || customMessage.isEmpty())
                ? resultCode.getMessage()
                : customMessage;
        this.data = data;
    }

    // ========================================================================
    // 成功回應便利方法
    // ========================================================================

    public static <T> Result<T> ok() {
        return new Result<>(ResultCodeEnum.OK, null, null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCodeEnum.OK, data, null);
    }

    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(ResultCodeEnum.OK, data, message);
    }

    // ========================================================================
    // 失敗回應便利方法
    // ========================================================================

    /**
     * 使用預設的 INTERNAL_ERROR 枚舉回傳錯誤
     */
    public static <T> Result<T> fail() {
        return new Result<>(ResultCodeEnum.INTERNAL_ERROR, null, null);
    }

    /**
     * 使用指定的枚舉回傳錯誤 (推薦使用)
     * 例如：Result.fail(ResultCodeEnum.PRODUCT_NOT_FOUND)
     */
    public static <T> Result<T> fail(ResultCodeEnum resultCode) {
        return new Result<>(resultCode, null, null);
    }

    /**
     * 使用指定的枚舉並附帶自訂訊息
     * 例如：Result.fail(ResultCodeEnum.STOCK_INSUFFICIENT, "商品 iPhone 15 庫存不足")
     */
    public static <T> Result<T> fail(ResultCodeEnum resultCode, String customMessage) {
        return new Result<>(resultCode, null, customMessage);
    }

    /**
     * 回傳一個包含自訂錯誤訊息的失敗結果。
     * code 會使用預設的 INTERNAL_ERROR。
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCodeEnum.INTERNAL_ERROR.getCode(), message, null);
    }

    /**
     * 回傳一個包含自訂字串 code 和 message 的失敗結果
     */
    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    // ========================================================================
    // 工具方法
    // ========================================================================

    /**
     * 判斷是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return ResultCodeEnum.OK.getCode().equals(this.code);
    }

    /**
     * 判斷是否失敗
     */
    @JsonIgnore
    public boolean isFailed() {
        return !isSuccess();
    }
}
