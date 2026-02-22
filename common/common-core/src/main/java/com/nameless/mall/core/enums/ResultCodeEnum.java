package com.nameless.mall.core.enums;

import lombok.Getter;

/**
 * API 回應狀態碼 Enum
 * 
 * 定義整個專案的統一錯誤碼標準。
 * 
 * 設計參考業界主流 API 錯誤模型 (Google Cloud, Stripe)。
 * 
 * 設計原則:
 * 1. Enum 名稱即錯誤碼: getCode() 回傳 this.name()，例如 "PRODUCT_NOT_FOUND"
 * 2. httpStatus 對應標準 HTTP 狀態碼: 用於設定 HTTP Response 狀態
 * 3. message 為繁體中文: 直接顯示給用戶
 * 
 * 使用範例:
 * - throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND);
 * - throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT, "iPhone 15
 * 庫存不足");
 * - return Result.fail(ResultCodeEnum.UNAUTHORIZED);
 * 
 * API 回應格式:
 * { "code": "PRODUCT_NOT_FOUND", "message": "商品不存在", "data": null }
 * 
 * @see Result
 * @see BusinessException
 */
@Getter
public enum ResultCodeEnum {

    // ========================================================================
    // 1. General/System (通用/系統) - HTTP 標準錯誤碼對應
    // ========================================================================
    /** 成功 - HTTP 200 */
    OK(200, "成功"),
    /** 參數無效 - HTTP 400 */
    INVALID_ARGUMENT(400, "參數無效"),
    /** 未認證 - HTTP 401 */
    UNAUTHORIZED(401, "未認證"),
    /** 無權限 - HTTP 403 */
    FORBIDDEN(403, "無權限"),
    /** 資源不存在 - HTTP 404 */
    NOT_FOUND(404, "資源不存在"),
    /** 資源已存在 - HTTP 409 */
    ALREADY_EXISTS(409, "資源已存在"),
    /** 請求過於頻繁 - HTTP 429 */
    RATE_LIMITED(429, "請求過於頻繁"),
    /** 系統內部錯誤 - HTTP 500 */
    INTERNAL_ERROR(500, "系統內部錯誤"),
    /** 服務暫時不可用 - HTTP 503 */
    SERVICE_UNAVAILABLE(503, "服務暫時不可用"),
    /** 操作逾時 - HTTP 504 */
    OPERATION_TIMEOUT(504, "操作逾時"),

    // ========================================================================
    // 2. Auth (認證)
    // ========================================================================
    CREDENTIALS_INVALID(401, "帳號或密碼錯誤"),
    TOKEN_EXPIRED(401, "Token 已過期"),
    TOKEN_INVALID(401, "Token 無效"),
    TOKEN_MISSING(401, "Token 缺失"),
    REFRESH_TOKEN_EXPIRED(401, "Refresh Token 已過期"),
    OAUTH_PROVIDER_ERROR(400, "OAuth 供應商錯誤"),
    OAUTH_STATE_MISMATCH(400, "OAuth 狀態不匹配"),
    CAPTCHA_INVALID(400, "驗證碼錯誤"),
    CAPTCHA_EXPIRED(400, "驗證碼已過期"),
    LOGIN_ATTEMPTS_EXCEEDED(429, "登入嘗試次數過多"),
    SESSION_EXPIRED(401, "Session 已過期"),
    MULTI_DEVICE_LOGIN(401, "帳號已在其他裝置登入"),

    // ========================================================================
    // 3. User (用戶)
    // ========================================================================
    USER_NOT_FOUND(404, "使用者不存在"),
    USERNAME_ALREADY_EXISTS(409, "使用者名稱已被註冊"),
    EMAIL_ALREADY_EXISTS(409, "電子郵件已被註冊"),
    PHONE_ALREADY_EXISTS(409, "手機號碼已被註冊"),
    PASSWORD_INCORRECT(400, "密碼不正確"),
    PASSWORD_TOO_WEAK(400, "密碼強度不足"),
    PASSWORD_SAME_AS_OLD(400, "新密碼不可與舊密碼相同"),
    EMAIL_NOT_VERIFIED(403, "電子郵件尚未驗證"),
    PHONE_NOT_VERIFIED(403, "手機號碼尚未驗證"),
    ACCOUNT_LOCKED(403, "帳號已鎖定"),
    ACCOUNT_DISABLED(403, "帳號已停用"),
    ACCOUNT_DELETED(410, "帳號已註銷"),
    ADDRESS_NOT_FOUND(404, "地址不存在"),
    ADDRESS_LIMIT_EXCEEDED(400, "地址數量已達上限"),
    PROFILE_UPDATE_FAILED(500, "個人資料更新失敗"),

    // ========================================================================
    // 4. Product (商品)
    // ========================================================================
    PRODUCT_NOT_FOUND(404, "商品不存在"),
    PRODUCT_UNAVAILABLE(400, "商品不可用"),
    PRODUCT_DELETED(410, "商品已刪除"),
    VARIANT_NOT_FOUND(404, "商品規格不存在"),
    VARIANT_UNAVAILABLE(400, "商品規格不可用"),
    CATEGORY_NOT_FOUND(404, "分類不存在"),
    BRAND_NOT_FOUND(404, "品牌不存在"),
    STOCK_INSUFFICIENT(400, "庫存不足"),
    STOCK_LOCKED(400, "庫存已被鎖定"),
    STOCK_DEDUCT_FAILED(500, "庫存扣減失敗"),
    STOCK_RETURN_FAILED(500, "庫存返還失敗"),
    PRICE_CHANGED(409, "商品價格已變更"),
    SKU_DUPLICATE(409, "SKU 編號重複"),
    PRODUCT_LIMIT_EXCEEDED(400, "商品購買數量超過限制"),

    // ========================================================================
    // 5. Cart (購物車)
    // ========================================================================
    CART_EMPTY(400, "購物車為空"),
    CART_ITEM_NOT_FOUND(404, "購物車項目不存在"),
    CART_ITEM_LIMIT_EXCEEDED(400, "購物車商品種類超過上限"),
    CART_QUANTITY_LIMIT_EXCEEDED(400, "單品數量超過上限"),
    CART_TOTAL_LIMIT_EXCEEDED(400, "購物車金額超過上限"),
    CART_ITEM_UNAVAILABLE(400, "購物車中有商品已下架"),
    CART_MERGE_FAILED(500, "購物車合併失敗"),
    CART_SYNC_FAILED(500, "購物車同步失敗"),

    // ========================================================================
    // 6. Order (訂單)
    // ========================================================================
    ORDER_NOT_FOUND(404, "訂單不存在"),
    ORDER_ACCESS_DENIED(403, "無權訪問此訂單"),
    ORDER_STATUS_INVALID(400, "訂單狀態不允許此操作"),
    ORDER_DUPLICATE(400, "請勿重複提交訂單"),
    ORDER_ALREADY_PAID(400, "訂單已支付"),
    ORDER_ALREADY_CANCELLED(400, "訂單已取消"),
    ORDER_CANNOT_CANCEL(400, "訂單無法取消"),
    ORDER_CANNOT_REFUND(400, "訂單無法退款"),
    ORDER_PAYMENT_EXPIRED(400, "訂單支付已逾時"),
    ORDER_SHIPPING_REQUIRED(400, "請填寫收貨地址"),
    ORDER_RECEIVER_REQUIRED(400, "請填寫收貨人資訊"),
    ORDER_ITEM_UNAVAILABLE(400, "訂單中有商品已下架"),
    ORDER_AMOUNT_MISMATCH(400, "訂單金額不符"),
    ORDER_ALREADY_SHIPPED(400, "訂單已發貨"),
    ORDER_ALREADY_COMPLETED(400, "訂單已完成"),
    ORDER_REVIEW_EXISTS(400, "已評價過此訂單"),
    ORDER_REVIEW_EXPIRED(400, "評價期限已過"),
    ORDER_CREATE_FAILED(500, "訂單創建失敗"),
    ORDER_CANCEL_FAILED(500, "訂單取消失敗"),

    // ========================================================================
    // 7. Payment (支付)
    // ========================================================================
    PAYMENT_NOT_FOUND(404, "支付單不存在"),
    PAYMENT_ACCESS_DENIED(403, "無權操作此支付單"),
    PAYMENT_STATUS_INVALID(400, "支付單狀態不允許此操作"),
    PAYMENT_FAILED(400, "支付失敗"),
    PAYMENT_TIMEOUT(400, "支付逾時"),
    PAYMENT_CANCELLED(400, "支付已取消"),
    PAYMENT_AMOUNT_MISMATCH(400, "支付金額不符"),
    PAYMENT_METHOD_INVALID(400, "支付方式無效"),
    PAYMENT_CHANNEL_ERROR(502, "支付渠道錯誤"),
    PAYMENT_CREATE_FAILED(500, "支付單創建失敗"),
    PAYMENT_CANCEL_FAILED(500, "支付單取消失敗"),
    REFUND_NOT_FOUND(404, "退款單不存在"),
    REFUND_FAILED(400, "退款失敗"),
    REFUND_AMOUNT_EXCEEDED(400, "退款金額超過可退金額"),

    // ========================================================================
    // 8. Coupon (優惠券)
    // ========================================================================
    COUPON_NOT_FOUND(404, "優惠券不存在"),
    COUPON_EXPIRED(400, "優惠券已過期"),
    COUPON_NOT_STARTED(400, "優惠券尚未生效"),
    COUPON_ALREADY_USED(400, "優惠券已使用"),
    COUPON_ALREADY_CLAIMED(400, "您已領取過此優惠券"),
    COUPON_CLAIM_LIMIT_REACHED(400, "優惠券領取次數已達上限"),
    COUPON_USAGE_LIMIT_REACHED(400, "優惠券使用次數已達上限"),
    COUPON_CONDITION_NOT_MET(400, "未滿足優惠券使用條件"),
    COUPON_AMOUNT_NOT_MET(400, "未達優惠券最低消費金額"),
    COUPON_CATEGORY_NOT_MET(400, "商品分類不適用此優惠券"),
    COUPON_PRODUCT_NOT_MET(400, "商品不適用此優惠券"),
    COUPON_CONFLICT(400, "優惠券不可疊加使用"),
    COUPON_USE_FAILED(500, "優惠券核銷失敗"),

    // ========================================================================
    // 9. Promotion (促銷/秒殺)
    // ========================================================================
    PROMOTION_NOT_FOUND(404, "活動不存在"),
    PROMOTION_NOT_STARTED(400, "活動尚未開始"),
    PROMOTION_ENDED(400, "活動已結束"),
    PROMOTION_SKU_NOT_FOUND(404, "特賣商品不存在"),
    PROMOTION_STOCK_INSUFFICIENT(400, "特賣商品庫存不足"),
    PROMOTION_SOLD_OUT(400, "活動商品已售罄"),
    PROMOTION_LIMIT_EXCEEDED(400, "超過活動限購數量"),
    PROMOTION_ALREADY_PARTICIPATED(400, "您已參與過此活動"),
    PROMOTION_QUEUE_FULL(503, "活動排隊人數已滿"),
    PROMOTION_PRICE_CHANGED(409, "活動價格已變更"),

    // ========================================================================
    // 10. Search (搜尋)
    // ========================================================================
    SEARCH_QUERY_EMPTY(400, "搜尋關鍵字為空"),
    SEARCH_QUERY_TOO_LONG(400, "搜尋關鍵字過長"),
    SEARCH_QUERY_INVALID(400, "搜尋關鍵字包含非法字符"),
    SEARCH_INDEX_ERROR(500, "搜尋索引錯誤"),
    SEARCH_TIMEOUT(504, "搜尋逾時");

    /**
     * 對應的 HTTP 狀態碼
     */
    private final int httpStatus;

    /**
     * 繁體中文錯誤訊息
     */
    private final String message;

    /**
     * 建構子
     */
    ResultCodeEnum(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 取得字串錯誤碼 (直接使用 Enum 名稱)
     * 例如: PRODUCT_NOT_FOUND, UNAUTHORIZED
     */
    public String getCode() {
        return this.name();
    }
}
