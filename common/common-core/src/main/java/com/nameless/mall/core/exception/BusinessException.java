package com.nameless.mall.core.exception;

import com.nameless.mall.core.enums.ResultCodeEnum;
import lombok.Getter;
import lombok.ToString;

/**
 * 可預期的業務邏輯錯誤，使用 ResultCodeEnum 建構。
 * GlobalExceptionHandler 會捕獲此例外並回傳對應的 HTTP status 和 Result。
 */
@Getter
@ToString
public final class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 對應的 ResultCodeEnum 枚舉值。
     */
    private final ResultCodeEnum resultCode;

    /**
     * 字串錯誤碼 (SCREAMING_SNAKE_CASE)。
     * <p>
     * 例如："PRODUCT_NOT_FOUND", "UNAUTHORIZED", "STOCK_INSUFFICIENT"
     */
    private final String code;

    /**
     * 對應的 HTTP 狀態碼。
     * <p>
     * GlobalExceptionHandler 會使用此值設定 HTTP Response Status。
     */
    private final int httpStatus;

    /**
     * 人類可讀的錯誤訊息 (繁體中文)。
     * <p>
     * 可直接顯示給終端用戶。
     */
    private final String message;

    /**
     * 【推薦】使用 ResultCodeEnum 建構業務例外。
     * <p>
     * 錯誤訊息將使用 ResultCodeEnum 中定義的預設訊息。
     *
     * @param resultCode 錯誤碼枚舉
     */
    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
        this.message = resultCode.getMessage();
    }

    /**
     * 【推薦】使用 ResultCodeEnum + 自訂訊息建構業務例外。
     * <p>
     * 適用於需要提供更具體上下文的場景，例如包含商品名稱或數量。
     *
     * @param resultCode    錯誤碼枚舉
     * @param customMessage 自訂錯誤訊息
     */
    public BusinessException(ResultCodeEnum resultCode, String customMessage) {
        super(customMessage);
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
        this.message = customMessage;
    }

}
