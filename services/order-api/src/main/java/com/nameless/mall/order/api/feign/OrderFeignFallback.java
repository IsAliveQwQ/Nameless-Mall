package com.nameless.mall.order.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * OrderFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class OrderFeignFallback implements FallbackFactory<OrderFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderFeignFallback.class);

    @Override
    public OrderFeignClient create(Throwable cause) {
        return orderSn -> {
            log.error("降級 | OrderFeignClient.getOrderDetail 失敗, orderSn: {}, cause: {}",
                    orderSn, cause.getMessage(), cause);
            return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "訂單服務暫時不可用，請稍後重試");
        };
    }
}
