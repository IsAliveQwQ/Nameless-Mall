package com.nameless.mall.cart.service;

import com.nameless.mall.cart.api.dto.CartDTO;
import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.cart.api.vo.CartVO;

import java.util.List;

/**
 * 購物車服務的接口
 * <p>
 * 定義了所有與購物車相關的業務操作。
 */
public interface CartService {

    /**
     * 將指定的商品規格 (SKU) 加入到目前登入使用者的購物車中。
     * <p>
     * 如果購物車中已存在該商品，則增加其數量；
     * 如果不存在，則新增一個購物車項目。
     *
     * @param variantId 要加入的商品規格 ID
     * @param quantity  要加入的數量
     * @return 更新後的完整購物車內容 (VO)
     */
    CartVO addToCart(Long userId, Long variantId, Integer quantity);

    /**
     * 獲取目前登入使用者的完整購物車內容。
     *
     * @return 包含所有購物車項目、總數量和總金額的 CartVO
     */
    CartVO getCart(Long userId);

    /**
     * 獲取原始數據內容 (用於內部處理或轉換)
     */
    CartDTO getCartDTO(Long userId);

    /**
     * 更新購物車中某個商品的數量。
     *
     * @param variantId 要更新的商品規格 ID
     * @param quantity  新的數量 (如果為 0，則應移除該商品)
     * @return 更新後的完整購物車內容
     */
    CartVO updateItemQuantity(Long userId, Long variantId, Integer quantity);

    /**
     * 從購物車中移除一個或多個商品項目。
     *
     * @param variantIds 要移除的商品規格 ID 列表
     * @return 更新後的完整購物車內容
     */
    CartVO removeItem(Long userId, Long... variantIds);

    /**
     * 清空目前登入使用者的購物車。
     */
    void clearCart(Long userId);

    List<CartItemDTO> getCartItemsByIds(Long userId, List<Long> cartItemIds);

    /**
     * 根據購物車項目 ID 列表，從購物車中清除對應的項目。
     * <p>
     * 專為內部 Feign 呼叫設計。
     *
     * @param cartItemIds 購物車項目 ID 列表 (這裡通常是指 variantId)
     */
    void clearCartItems(Long userId, List<Long> cartItemIds);

}
