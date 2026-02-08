package com.nameless.mall.payment.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 支付方式枚舉
 * 
 * @author Nameless Mall Team
 * @since v4.3.0
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    // === 綠界 ECPay ===
    ECPAY_CREDIT("綠界信用卡", PaymentProvider.ECPAY),
    ECPAY_ATM("綠界虛擬帳號", PaymentProvider.ECPAY),
    ECPAY_CVS("綠界超商代碼", PaymentProvider.ECPAY),

    // === LINE Pay ===
    LINE_PAY("LINE Pay", PaymentProvider.LINEPAY),

    // === 傳統方式 (手動確認) ===
    BANK_TRANSFER("銀行轉帳", PaymentProvider.MANUAL),
    COD("貨到付款", PaymentProvider.MANUAL);

    /**
     * 顯示名稱 (前端顯示用)
     */
    private final String displayName;

    /**
     * 所屬金流提供商
     */
    private final PaymentProvider provider;

    /**
     * 從舊版整數值轉換 (向後相容)
     * 
     * @param legacyCode 舊版代碼 (1=銀行轉帳, 2=貨到付款)
     * @return 對應的 PaymentMethod
     */
    public static PaymentMethod fromLegacyCode(Integer legacyCode) {
        if (legacyCode == null) {
            return null;
        }
        return switch (legacyCode) {
            case 1 -> LINE_PAY;
            case 2 -> ECPAY_CREDIT;
            default -> null;
        };
    }

    /**
     * 轉換為舊版整數值 (向後相容)
     */
    public Integer toLegacyCode() {
        return switch (this) {
            case LINE_PAY -> 1;
            case ECPAY_CREDIT -> 2;
            case ECPAY_ATM -> 2;
            case ECPAY_CVS -> 2;
            default -> null;
        };
    }
}
