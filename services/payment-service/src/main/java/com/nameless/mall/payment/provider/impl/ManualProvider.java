package com.nameless.mall.payment.provider.impl;

import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentProvider;
import com.nameless.mall.payment.api.enums.PaymentStatus;
import com.nameless.mall.payment.provider.AbstractPaymentProvider;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;
import com.nameless.mall.payment.provider.dto.PaymentInitContext;
import com.nameless.mall.payment.provider.dto.PaymentInitResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 手動確認支付提供商
 * 
 * <p>
 * 支援銀行轉帳與貨到付款，無需第三方金流導向。
 * </p>
 * 
 */
@Component
public class ManualProvider extends AbstractPaymentProvider {

    /**
     * 銀行轉帳有效期 (小時)
     */
    private static final int BANK_TRANSFER_EXPIRE_HOURS = 48;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MANUAL;
    }

    @Override
    public List<PaymentMethod> getSupportedMethods() {
        return List.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.COD);
    }

    @Override
    protected PaymentInitResult doInitPayment(PaymentInitContext context) {
        LocalDateTime expireAt;

        // 貨到付款無有效期限制
        if (context.getMethod() == PaymentMethod.COD) {
            expireAt = null;
        } else {
            // 銀行轉帳預設 48 小時
            expireAt = LocalDateTime.now().plusHours(BANK_TRANSFER_EXPIRE_HOURS);
        }

        return PaymentInitResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .redirectType(PaymentInitResult.RedirectType.NONE)
                .expireAt(expireAt)
                .build();
    }

    @Override
    protected PaymentCallbackResult doHandleCallback(Map<String, String> params) {
        // 手動確認：從參數取得帳戶資訊
        String accountInfo = params.get("accountInfo");

        return PaymentCallbackResult.builder()
                .success(true)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .providerResponse(Map.of("accountInfo", accountInfo != null ? accountInfo : "").toString())
                .build();
    }
}
