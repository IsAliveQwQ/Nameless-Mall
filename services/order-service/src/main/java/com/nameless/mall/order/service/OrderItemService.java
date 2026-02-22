package com.nameless.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.order.api.dto.OrderItemDTO;
import com.nameless.mall.order.entity.OrderItem;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;

import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;

import java.util.List;
import java.util.Map;

/**
 * 訂單項目服務的接口
 * <p>
 * 繼承 IService<OrderItem> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 * 提供訂單項目相關的業務邏輯。
 */
public interface OrderItemService extends IService<OrderItem> {

        /**
         * 根據購物車項目與計價結果構建訂單項目列表（純記憶體操作，不存檔）。
         * 用於事務外預先組裝，縮小事務範圍。orderId 待事務內回填。
         *
         * @param orderSn    訂單業務編號
         * @param cartItems  購物車項目
         * @param pricingMap 促銷計價結果 (可為 null)
         * @return 未存檔的 OrderItem 列表
         */
        List<OrderItem> buildOrderItems(String orderSn, List<CartItemDTO> cartItems,
                        Map<Long, ProductPriceResultDTO> pricingMap);

        /**
         * 根據訂單 ID 查詢訂單項目
         * 
         * @param orderId 訂單 ID
         * @return 訂單項目列表
         */
        List<OrderItem> getByOrderId(Long orderId);

        /**
         * 將訂單項目列表轉換為庫存扣減 DTO 列表
         * 
         * @param orderItems 訂單項目
         * @return 庫存扣減 DTO 列表
         */
        List<DecreaseStockInputDTO> buildStockDTOList(List<OrderItem> orderItems);

        /**
         * 將訂單項目列表轉換為 DTO 列表
         * 
         * @param orderItems 訂單項目
         * @return DTO 列表
         */
        List<OrderItemDTO> toDTO(List<OrderItem> orderItems);
}
