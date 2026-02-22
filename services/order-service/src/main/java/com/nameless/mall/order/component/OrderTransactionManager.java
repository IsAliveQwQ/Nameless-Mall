package com.nameless.mall.order.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.order.api.enums.OrderStatus;
import com.nameless.mall.order.entity.Order;
import com.nameless.mall.order.entity.OrderItem;
import com.nameless.mall.order.entity.OrderShipment;
import com.nameless.mall.order.mapper.OrderMapper;

import com.nameless.mall.order.service.OrderItemService;
import com.nameless.mall.order.service.OrderShipmentService;
import com.nameless.mall.order.service.ReliableMessageService;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import com.nameless.mall.promotion.api.feign.PromotionFeignClient;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 訂單事務管理器，負責下單寫入、庫存扣減等事務操作。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTransactionManager {

    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final OrderShipmentService orderShipmentService;
    private final PromotionFeignClient promotionFeignClient;
    private final ReliableMessageService reliableMessageService;

    /** 完成異步下單：更新訂單為 PENDING_PAYMENT + 寫入明細 + Outbox。 */
    @Transactional(rollbackFor = Exception.class)
    public void completeAsyncOrder(Order order, List<OrderItem> orderItems, OrderShipment shipment,
            List<CartItemDTO> cartItems, Map<Long, ProductPriceResultDTO> pricingMap, Long userCouponId) {

        // 1. 更新訂單主表狀態：CREATING → PENDING_PAYMENT
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 2. 回填 orderId 並批量寫入訂單明細和物流資訊
        Long orderId = order.getId();
        orderItems.forEach(item -> item.setOrderId(orderId));
        shipment.setOrderId(orderId);
        orderItemService.saveBatch(orderItems);
        orderShipmentService.save(shipment);

        // 3. 寫入特賣記錄（唯一約束 userId+promotionId+skuId 防重複購買）
        for (CartItemDTO cartItem : cartItems) {
            ProductPriceResultDTO priceResult = pricingMap.get(cartItem.getVariantId());
            if (priceResult != null && "FLASH_SALE".equals(priceResult.getPromotionType())
                    && priceResult.getPromotionId() != null) {
                orderMapper.insertFlashSaleRecord(
                        order.getUserId(),
                        priceResult.getPromotionId(),
                        cartItem.getVariantId(),
                        order.getOrderSn(),
                        cartItem.getQuantity());
            }
        }

        // 4. 寫入 Outbox 消息（優惠券核銷、訂單建立通知、延遲取消任務）
        if (userCouponId != null) {
            reliableMessageService.createCouponUseMessage(userCouponId, order.getOrderSn());
        }

        List<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
        reliableMessageService.createOrderCreatedMessage(orderId, productIds);
        reliableMessageService.createOrderDelayMessage(order.getOrderSn());
    }

    /** 標記訂單為 CREATE_FAILED（CAS）。 */
    @Transactional(rollbackFor = Exception.class)
    public void markOrderFailed(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order != null && OrderStatus.CREATING.getCode().equals(order.getStatus())) {
            order.setStatus(OrderStatus.CREATE_FAILED.getCode());
            order.setFailReason(reason);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("【訂單失敗】orderId={}, reason={}", orderId, reason);
        }
    }

    public void deductFlashSaleStock(Order order, List<CartItemDTO> cartItems,
            Map<Long, ProductPriceResultDTO> pricingMap) {

        if (pricingMap == null || pricingMap.isEmpty())
            return;

        // 1. 過濾出屬於特賣的商品，組裝批量扣減 DTO
        List<FlashSaleDeductionDTO> deductionList = cartItems.stream()
                .filter(item -> pricingMap.containsKey(item.getVariantId()))
                .map(item -> {
                    ProductPriceResultDTO promoResult = pricingMap.get(item.getVariantId());
                    if ("FLASH_SALE".equals(promoResult.getPromotionType()) && promoResult.getPromotionId() != null) {
                        FlashSaleDeductionDTO dto = new FlashSaleDeductionDTO();
                        dto.setPromotionId(promoResult.getPromotionId());
                        dto.setProductId(item.getProductId());
                        dto.setSkuId(item.getVariantId());
                        dto.setQuantity(item.getQuantity());
                        dto.setUserId(order.getUserId());
                        dto.setOrderSn(order.getOrderSn());
                        return dto;
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 2. 透過 Feign 呼叫 promotion-service 執行特賣庫存扣減
        if (!deductionList.isEmpty()) {
            log.info("【特賣扣減】發送扣減請求 (Unified): Count={}", deductionList.size());
            Result<Void> result = promotionFeignClient.deductStock(deductionList);
            if (result == null || !result.isSuccess()) {
                String errorMsg = result != null ? result.getMessage() : "服務無回應";
                log.error("【特賣扣減失敗】觸發回滾: {}", errorMsg);
                throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT, "特賣庫存不足或已超過限購: " + errorMsg);
            }
        }
    }

    public void revertFlashSaleStockIfAny(String orderSn) {

        try {
            promotionFeignClient.recoverStock(orderSn);
        } catch (Exception e) {
            log.error("【特賣庫存返還失敗】orderSn={}, error={}", orderSn, e.getMessage());
        }
    }

}
