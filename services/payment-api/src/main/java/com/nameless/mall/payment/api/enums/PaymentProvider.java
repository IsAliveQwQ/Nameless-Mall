package com.nameless.mall.payment.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 金流提供商枚舉。
 */
@Getter
@RequiredArgsConstructor
public enum PaymentProvider {

    /**
     * 手動確認 (銀行轉帳、貨到付款)
     */
    MANUAL("手動確認", false),

    /**
     * 綠界 ECPay (台灣主流)
     */
    ECPAY("綠界科技", true),

    /**
     * LINE Pay (亞洲主流)
     */
    LINEPAY("LINE Pay", true);

    /**
     * 提供商名稱
     */
    private final String displayName;

    /**
     * 是否為第三方金流 (需跳轉)
     */
    private final boolean thirdParty;

    /**
     * 是否需要跳轉至外部頁面
     */
    public boolean requiresRedirect() {
        return thirdParty;
    }
}
