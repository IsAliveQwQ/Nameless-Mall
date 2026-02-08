package com.nameless.mall.cart.api.feign;

import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 購物車服務 Feign Client
 */
@FeignClient(name = "cart-service", fallbackFactory = CartFeignFallback.class)
public interface CartFeignClient {

    /**
     * 根據購物車項目 ID 列表，查詢對應的購物車項目。
     * <p>
     * 這是專門為訂單服務在建立訂單時，獲取已勾選商品資訊而設計的內部接口。
     *
     * @param cartItemIds 一個包含多個購物車項目 ID 的列表
     * @return 包含查詢到的購物車項目列表的 Result 物件
     */
    @PostMapping("/cart/internal/items")
    Result<List<CartItemDTO>> getCartItemsByIds(@RequestBody List<Long> cartItemIds);

    /**
     * 根據購物車項目 ID 列表，從購物車中清除對應的項目。
     * <p>
     * 專門為訂單服務在訂單建立成功後，清空購物車而設計的內部接口。
     *
     * @param cartItemIds 一個包含多個購物車項目 ID 的列表
     * @return 操作結果
     */
    @PostMapping("/cart/internal/clear-items")
    Result<Void> clearCartItems(@RequestBody List<Long> cartItemIds);

}
