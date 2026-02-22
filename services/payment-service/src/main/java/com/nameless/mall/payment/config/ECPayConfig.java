package com.nameless.mall.payment.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 綠界金流 (ECPay) 配置類
 * 
 * <p>
 * 配置來源：Nacos payment-service.yml
 * </p>
 * 
 * <p>
 * 測試環境憑證（綠界官方提供）：
 * <ul>
 * <li>MerchantID: 2000132</li>
 * <li>HashKey: 5294y06JbISpM5x9</li>
 * <li>HashIV: v77hoKGq4kWxNNIS</li>
 * </ul>
 * </p>
 * 
 * @see <a href="https://developers.ecpay.com.tw">綠界開發者文檔</a>
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ecpay")
public class ECPayConfig {

    /**
     * 商店代號
     */
    private String merchantId = "2000132";

    /**
     * HashKey (用於 CheckMacValue)
     */
    private String hashKey = "5294y06JbISpM5x9";

    /**
     * HashIV (用於 CheckMacValue)
     */
    private String hashIv = "v77hoKGq4kWxNNIS";

    /**
     * 是否為測試環境
     */
    private boolean sandbox = true;

    /**
     * 支付結果回調 URL (後端接收)
     */
    private String callbackUrl;

    /**
     * 前端返回 URL (用戶完成支付後)
     */
    private String frontendReturnUrl;

    /**
     * 取得 API 基礎 URL
     */
    public String getApiBaseUrl() {
        return sandbox
                ? "https://payment-stage.ecpay.com.tw"
                : "https://payment.ecpay.com.tw";
    }

    /**
     * 取得 AioCheckOut 完整 URL
     */
    public String getAioCheckOutUrl() {
        return getApiBaseUrl() + "/Cashier/AioCheckOut/V5";
    }

    /**
     * 驗證配置完整性（由 ECPayProvider 呼叫）
     * 
     * @throws IllegalStateException 當必填配置缺失時
     */
    public void validate() {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalStateException("[ECPay] 配置錯誤：ecpay.callback-url 為必填");
        }
        if (frontendReturnUrl == null || frontendReturnUrl.isBlank()) {
            throw new IllegalStateException("[ECPay] 配置錯誤：ecpay.frontend-return-url 為必填");
        }
        log.info("[ECPay] 配置驗證通過 - MerchantID: {}, Sandbox: {}", merchantId, sandbox);
    }
}
