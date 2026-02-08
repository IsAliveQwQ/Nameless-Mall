package com.nameless.mall.cart.api.feign;

import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CartFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class CartFeignFallback implements FallbackFactory<CartFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(CartFeignFallback.class);

    @Override
    public CartFeignClient create(Throwable cause) {
        return new CartFeignClient() {
            @Override
            public Result<List<CartItemDTO>> getCartItemsByIds(List<Long> cartItemIds) {
                log.error("降級 | CartFeignClient.getCartItemsByIds 失敗, 數量: {}, cause: {}",
                        cartItemIds != null ? cartItemIds.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "購物車服務暫時不可用，請稍後重試");
            }

            @Override
            public Result<Void> clearCartItems(List<Long> cartItemIds) {
                log.error("降級 | CartFeignClient.clearCartItems 失敗, 數量: {}, cause: {}",
                        cartItemIds != null ? cartItemIds.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "購物車服務暫時不可用，清除操作將稍後重試");
            }
        };
    }
}
