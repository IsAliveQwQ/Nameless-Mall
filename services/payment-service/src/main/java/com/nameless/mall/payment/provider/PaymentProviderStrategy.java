package com.nameless.mall.payment.provider;

import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentProvider;
import com.nameless.mall.payment.provider.dto.PaymentInitContext;
import com.nameless.mall.payment.provider.dto.PaymentInitResult;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;

import java.util.List;
import java.util.Map;

/**
 * 支付提供商策略介面
 * 
 * <p>
 * 定義第三方金流整合的標準契約，採用 Strategy Pattern 實現多金流商解耦。
 * </p>
 * 
 * @see PaymentProvider
 */
public interface PaymentProviderStrategy {

    /**
     * 取得提供商標識
     * 
     * @return 提供商枚舉 (MANUAL, ECPAY, LINEPAY)
     */
    PaymentProvider getProvider();

    /**
     * 取得支援的付款方式
     * 
     * @return 支援的付款方式列表
     */
    List<PaymentMethod> getSupportedMethods();

    /**
     * 初始化支付並取得導向資訊
     * 
     * <p>
     * 對於第三方金流，回傳 redirectUrl 或 formData；
     * 對於手動確認，直接回傳 PENDING 狀態。
     * </p>
     * 
     * @param context 支付初始化上下文 (訂單資訊、金額等)
     * @return 初始化結果 (含導向 URL 或表單參數)
     */
    PaymentInitResult initPayment(PaymentInitContext context);

    /**
     * 處理回調/確認
     * 
     * <p>
     * 處理第三方異步通知 (Webhook) 或用戶確認操作。
     * </p>
     * 
     * @param params 回調參數 (第三方傳入的參數或用戶輸入)
     * @return 回調處理結果 (支付狀態、第三方交易編號等)
     */
    PaymentCallbackResult handleCallback(Map<String, String> params);

    /**
     * 查詢支付狀態 (向第三方主動查詢)
     * 
     * <p>
     * 預設不支援，由具體實作覆寫。
     * </p>
     * 
     * @param providerTradeNo 第三方交易編號
     * @return 查詢結果
     * @throws UnsupportedOperationException 若提供商不支援此操作
     */
    default PaymentCallbackResult queryStatus(String providerTradeNo) {
        throw new UnsupportedOperationException(
                getProvider().name() + " 不支援主動查詢支付狀態");
    }
}
