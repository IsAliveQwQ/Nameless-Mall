package com.nameless.mall.payment.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.payment.api.dto.PaymentCreateDTO;
import com.nameless.mall.payment.api.dto.PaymentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * PaymentFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class PaymentFeignFallback implements FallbackFactory<PaymentFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(PaymentFeignFallback.class);

    @Override
    public PaymentFeignClient create(Throwable cause) {
        return new PaymentFeignClient() {
            @Override
            public Result<PaymentDTO> createPayment(PaymentCreateDTO dto) {
                log.error("降級 | PaymentFeignClient.createPayment 失敗, orderSn: {}, cause: {}",
                        dto != null ? dto.getOrderSn() : "null", cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "支付服務暫時不可用，請稍後再試");
            }

            @Override
            public Result<PaymentDTO> getPaymentByOrderSn(String orderSn) {
                log.error("降級 | PaymentFeignClient.getPaymentByOrderSn 失敗, orderSn: {}, cause: {}",
                        orderSn, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "支付服務暫時不可用，請稍後再試");
            }

            @Override
            public Result<Void> cancelPayment(String orderSn) {
                log.error("降級 | PaymentFeignClient.cancelPayment 失敗, orderSn: {}, cause: {}",
                        orderSn, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "支付服務暫時不可用，請稍後再試");
            }
        };
    }
}
