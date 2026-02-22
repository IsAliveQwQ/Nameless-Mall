package com.nameless.mall.payment.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 支付狀態枚舉
 * 
 * <pre>
 * 狀態機流轉:
 * ┌─────────┐    create    ┌────────────┐    redirect    ┌────────────┐
 * │ (start) │ ──────────→  │  PENDING   │ ────────────→  │ PROCESSING │
 * └─────────┘              └────────────┘                └────────────┘
 *                               │                              │
 *                          expire/cancel                   callback
 *                               ↓                              ↓
 *                         ┌───────────┐               ┌────────────────┐
 *                         │ CANCELLED │               │ SUCCESS/FAILED │
 *                         │ EXPIRED   │               └────────────────┘
 *                         └───────────┘
 * </pre>
 * 
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    /**
     * 待支付 - 支付單已建立，等待用戶付款
     */
    PENDING(0, "待支付"),

    /**
     * 處理中 - 用戶已跳轉至第三方，等待回調
     */
    PROCESSING(1, "處理中"),

    /**
     * 支付成功 - 已收到款項
     */
    SUCCESS(2, "支付成功"),

    /**
     * 支付失敗 - 第三方回報失敗
     */
    FAILED(3, "支付失敗"),

    /**
     * 已過期 - 超過有效期限未完成付款
     */
    EXPIRED(4, "已過期"),

    /**
     * 已取消 - 用戶或系統主動取消
     */
    CANCELLED(5, "已取消"),

    /**
     * 已退款 - 款項已退還
     */
    REFUNDED(6, "已退款");

    /**
     * 數值代碼 (DB 存儲用)
     */
    private final int code;

    /**
     * 顯示名稱
     */
    private final String displayName;

    /**
     * 從數值代碼轉換
     */
    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 從舊版狀態碼轉換 (向後相容)
     * 舊版: 0=待支付, 1=已支付, 2=已退款, 3=已取消
     */
    public static PaymentStatus fromLegacyCode(Integer legacyCode) {
        if (legacyCode == null) {
            return null;
        }
        return switch (legacyCode) {
            case 0 -> PENDING;
            case 1 -> SUCCESS; // 舊版「已支付」對應新版 SUCCESS
            case 2 -> REFUNDED;
            case 3 -> CANCELLED;
            default -> null;
        };
    }

    /**
     * 是否為終態 (不可再變更)
     */
    public boolean isFinalState() {
        return this == SUCCESS || this == FAILED || this == EXPIRED
                || this == CANCELLED || this == REFUNDED;
    }

    /**
     * 是否可取消
     */
    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }
}
