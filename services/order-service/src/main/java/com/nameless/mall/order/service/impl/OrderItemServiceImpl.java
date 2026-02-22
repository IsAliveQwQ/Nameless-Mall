package com.nameless.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.order.api.dto.OrderItemDTO;
import com.nameless.mall.order.entity.OrderItem;
import com.nameless.mall.order.mapper.OrderItemMapper;
import com.nameless.mall.order.service.OrderItemService;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 訂單項目服務的實現類
 * <p>
 * 負責處理與 OrderItem 相關的業務邏輯。
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {

    @Override
    public List<OrderItem> buildOrderItems(String orderSn, List<CartItemDTO> cartItems,
            Map<Long, ProductPriceResultDTO> pricingMap) {
        return cartItems.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            // 1. 基本欄位填入
            orderItem.setOrderSn(orderSn);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setVariantId(cartItem.getVariantId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setProductImage(cartItem.getProductImage());
            orderItem.setQuantity(cartItem.getQuantity());

            // 2. 從 options 組合 SKU 名稱（如：白色 256G）
            orderItem.setSkuName(buildSkuName(cartItem));

            // 3. 填入促銷快照（原價、促銷價、折扣額）
            BigDecimal originalPrice = cartItem.getOriginalPrice() != null ? cartItem.getOriginalPrice()
                    : cartItem.getPrice();
            orderItem.setOriginalPrice(originalPrice);

            if (pricingMap != null && pricingMap.containsKey(cartItem.getVariantId())) {
                ProductPriceResultDTO promo = pricingMap.get(cartItem.getVariantId());
                if (promo.getFinalPrice() != null) {
                    orderItem.setProductPrice(promo.getFinalPrice());
                    orderItem.setPromotionName(promo.getPromotionName());
                    // 計算折扣額 = (原價 - 促銷價) × 數量（與購物車/結帳總計口徑一致）
                    BigDecimal unitDiscount = originalPrice.subtract(promo.getFinalPrice());
                    BigDecimal totalDiscount = unitDiscount.compareTo(BigDecimal.ZERO) > 0
                            ? unitDiscount.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                            : BigDecimal.ZERO;
                    orderItem.setPromotionAmount(totalDiscount);
                } else {
                    // 促銷價格為 null 時的回退
                    orderItem.setProductPrice(cartItem.getPrice());
                }
            } else {
                // 無促銷時使用購物車價格
                orderItem.setProductPrice(cartItem.getPrice());
            }

            return orderItem;
        }).collect(Collectors.toList());
    }

    /**
     * 從購物車項目的規格選項組合生成 SKU 名稱
     * 
     * @param cartItem 購物車項目
     * @return 規格名稱 (如：白色 256G)，若無選項則返回 null
     */
    private String buildSkuName(CartItemDTO cartItem) {
        if (cartItem.getOptions() == null || cartItem.getOptions().isEmpty()) {
            return null;
        }
        return cartItem.getOptions().stream()
                .map(opt -> opt.getOptionValue())
                .filter(value -> value != null && !value.isEmpty())
                .collect(Collectors.joining(" "));
    }

    @Override
    public List<OrderItem> getByOrderId(Long orderId) {
        return this.list(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    @Override
    public List<DecreaseStockInputDTO> buildStockDTOList(List<OrderItem> orderItems) {
        if (CollectionUtils.isEmpty(orderItems)) {
            return List.of();
        }
        return orderItems.stream()
                .map(item -> new DecreaseStockInputDTO(item.getVariantId(), item.getQuantity()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItemDTO> toDTO(List<OrderItem> orderItems) {
        if (CollectionUtils.isEmpty(orderItems)) {
            return List.of();
        }
        return orderItems.stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}
