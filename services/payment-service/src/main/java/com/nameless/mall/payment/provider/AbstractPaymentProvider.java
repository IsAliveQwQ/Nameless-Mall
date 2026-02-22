package com.nameless.mall.payment.provider;

import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;
import com.nameless.mall.payment.provider.dto.PaymentInitContext;
import com.nameless.mall.payment.provider.dto.PaymentInitResult;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付提供商抽象基底類別
 */
@Slf4j
public abstract class AbstractPaymentProvider implements PaymentProviderStrategy {

    @Override
    public final PaymentInitResult initPayment(PaymentInitContext context) {
        String provider = getProvider().name();
        log.info("【支付初始化】Provider={}, OrderSn={}, Amount={}", provider, context.getOrderSn(), context.getAmount());
        try {
            validateInitContext(context);
            return doInitPayment(context);
        } catch (BusinessException e) {
            log.warn("【支付初始化】業務異常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("【支付初始化】系統異常", e);
            throw new BusinessException(ResultCodeEnum.PAYMENT_CHANNEL_ERROR, "支付初始化失敗");
        }
    }

    @Override
    public final PaymentCallbackResult handleCallback(Map<String, String> params) {
        log.info("【支付回調】Provider={}, Params={}", getProvider().name(), params);
        try {
            return doHandleCallback(params);
        } catch (BusinessException e) {
            // 業務例外：傳遞到 Controller 層由 GlobalExceptionHandler 統一處理
            log.warn("【支付回調】業務異常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("【支付回調】系統異常", e);
            throw new BusinessException(ResultCodeEnum.PAYMENT_CHANNEL_ERROR,
                    "回調處理失敗: " + e.getMessage());
        }
    }

    protected abstract PaymentInitResult doInitPayment(PaymentInitContext context);

    protected abstract PaymentCallbackResult doHandleCallback(Map<String, String> params);

    protected void validateInitContext(PaymentInitContext context) {
        if (context.getAmount() == null || context.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "金額無效");
        }
        if (context.getOrderSn() == null || context.getOrderSn().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "訂單號缺失");
        }
    }
}
