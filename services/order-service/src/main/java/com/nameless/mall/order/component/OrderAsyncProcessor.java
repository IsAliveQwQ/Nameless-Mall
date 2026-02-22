package com.nameless.mall.order.component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.cart.api.feign.CartFeignClient;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.coupon.api.dto.CouponCalculationDTO;
import com.nameless.mall.coupon.api.dto.CouponCalculationResult;
import com.nameless.mall.coupon.api.feign.CouponFeignClient;
import com.nameless.mall.order.api.dto.OrderSubmitDTO;
import com.nameless.mall.order.api.enums.ShippingMethod;
import com.nameless.mall.order.config.ShippingProperties;
import com.nameless.mall.order.entity.Order;
import com.nameless.mall.order.entity.OrderItem;
import com.nameless.mall.order.entity.OrderShipment;
import com.nameless.mall.order.mapper.OrderMapper;
import com.nameless.mall.order.service.OrderItemService;
import com.nameless.mall.order.service.OrderShipmentService;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.feign.ProductFeignClient;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import com.nameless.mall.promotion.api.feign.PromotionFeignClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 訂單異步處理器，在獨立線程池完成計價、庫存扣減、明細寫入等核心流程。
 * <p>
 * 關鍵優化：coupon 試算、特賣庫存扣減、一般庫存扣減三路並行執行，
 * 將 ~300ms 串行 RPC 壓縮至 ~100ms 單輪並行。
 * </p>
 * <h3>執行緒模型</h3>
 * <ul>
 * <li>{@code @Async("orderAsyncExecutor")} 外層任務池：每筆訂單佔 1 個線程，阻塞等待子任務完成。</li>
 * <li>{@code CompletableFuture.xxxAsync(..., feignCallExecutor)} 內層 Feign RPC
 * 池：
 * 專門執行 Feign 遠程呼叫，與外層池完全隔離，杜絕線程飢餓。</li>
 * <li>異步線程不會自動繼承 SecurityContext，因此所有需要的參數（userId 等）
 * 都由呼叫方在切換線程前取得並傳入。</li>
 * </ul>
 */
@Slf4j
@Component
public class OrderAsyncProcessor {

    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final OrderShipmentService orderShipmentService;
    private final ProductFeignClient productFeignClient;
    private final CouponFeignClient couponFeignClient;
    private final ShippingProperties shippingProperties;
    private final OrderTransactionManager orderTransactionManager;
    private final CartFeignClient cartFeignClient;
    private final PromotionFeignClient promotionFeignClient;
    private final Executor feignCallExecutor;

    public OrderAsyncProcessor(
            OrderMapper orderMapper,
            OrderItemService orderItemService,
            OrderShipmentService orderShipmentService,
            ProductFeignClient productFeignClient,
            CouponFeignClient couponFeignClient,
            ShippingProperties shippingProperties,
            OrderTransactionManager orderTransactionManager,
            CartFeignClient cartFeignClient,
            PromotionFeignClient promotionFeignClient,
            @Qualifier("feignCallExecutor") Executor feignCallExecutor) {
        this.orderMapper = orderMapper;
        this.orderItemService = orderItemService;
        this.orderShipmentService = orderShipmentService;
        this.productFeignClient = productFeignClient;
        this.couponFeignClient = couponFeignClient;
        this.shippingProperties = shippingProperties;
        this.orderTransactionManager = orderTransactionManager;
        this.cartFeignClient = cartFeignClient;
        this.promotionFeignClient = promotionFeignClient;
        this.feignCallExecutor = feignCallExecutor;
    }

    /** 異步處理訂單核心流程。userId 由呼叫方傳入（異步線程無 SecurityContext）。 */
    @Async("orderAsyncExecutor")
    public void processOrderAsync(Long orderId, String orderSn, Long userId,
            OrderSubmitDTO submitDTO, List<CartItemDTO> cartItems) {
        List<DecreaseStockInputDTO> regularStockList = Collections.emptyList();
        boolean flashSaleDeducted = false;

        try {
            log.info("【異步下單】開始: orderSn={}, userId={}, items={}", orderSn, userId, cartItems.size());

            Map<Long, ProductPriceResultDTO> pricingMap = buildPricingMapFromCart(cartItems);

            Order order = orderMapper.selectById(orderId);
            recalculateOrderAmounts(order, cartItems, pricingMap);
            calculateShipping(order, submitDTO.getShippingMethod());

            List<OrderItem> orderItems = orderItemService.buildOrderItems(orderSn, cartItems, pricingMap);
            OrderShipment shipment = orderShipmentService.buildShipment(orderSn, submitDTO);
            regularStockList = buildRegularStockList(orderItems, pricingMap);

            // 並行 Feign RPC：coupon 試算 + 特賣扣庫存 + 一般扣庫存
            // 使用獨立的 feignCallExecutor，避免與外層 @Async 共用線程池導致飢餓
            final List<DecreaseStockInputDTO> stockListForLambda = regularStockList;

            CompletableFuture<CouponCalculationResult> couponFuture = (submitDTO.getUserCouponId() != null)
                    ? CompletableFuture.supplyAsync(() -> callCouponCalculation(order, submitDTO, userId),
                            feignCallExecutor)
                    : CompletableFuture.completedFuture(null);

            CompletableFuture<Void> flashSaleFuture = CompletableFuture.runAsync(
                    () -> orderTransactionManager.deductFlashSaleStock(order, cartItems, pricingMap),
                    feignCallExecutor);

            CompletableFuture<Void> regularStockFuture = (!stockListForLambda.isEmpty())
                    ? CompletableFuture.runAsync(() -> deductRegularStock(stockListForLambda), feignCallExecutor)
                    : CompletableFuture.completedFuture(null);

            // 等待三路全部完成（任一失敗則整體失敗）
            CompletableFuture.allOf(couponFuture, flashSaleFuture, regularStockFuture).join();

            // 三路都成功才走到這裡
            flashSaleDeducted = true;
            CouponCalculationResult couponResult = couponFuture.join();
            applyCouponResult(order, couponResult);

            orderTransactionManager.completeAsyncOrder(
                    order, orderItems, shipment, cartItems, pricingMap, submitDTO.getUserCouponId());

            // 清購物車（容錯執行）
            try {
                cartFeignClient.clearCartItems(submitDTO.getCartItemIds());
            } catch (Exception e) {
                log.warn("【異步下單】購物車清理失敗（不影響訂單）: orderSn={}", orderSn, e);
            }

            log.info("【異步下單】完成: orderSn={}, payAmount={}", orderSn, order.getPayAmount());

        } catch (CompletionException ce) {
            Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
            log.error("【異步下單】並行階段失敗，執行補償: orderSn={}", orderSn, cause);
            compensate(orderId, orderSn, regularStockList, flashSaleDeducted, cause);
        } catch (Exception e) {
            log.error("【異步下單】失敗，執行補償: orderSn={}", orderSn, e);
            compensate(orderId, orderSn, regularStockList, flashSaleDeducted, e);
        }
    }

    /** Feign→coupon-service 試算優惠券（獨立執行，不依賴庫存結果）。 */
    private CouponCalculationResult callCouponCalculation(Order order, OrderSubmitDTO submitDTO, Long userId) {
        CouponCalculationDTO calcDTO = new CouponCalculationDTO();
        calcDTO.setUserId(userId);
        calcDTO.setUserCouponId(submitDTO.getUserCouponId());
        calcDTO.setOrderTotalAmount(order.getTotalAmount());
        calcDTO.setShippingFee(order.getShippingFee());

        Result<CouponCalculationResult> calcResult = couponFeignClient.calculateDiscount(calcDTO);

        if (calcResult != null && calcResult.isSuccess() && calcResult.getData() != null) {
            return calcResult.getData();
        }
        throw new BusinessException(ResultCodeEnum.COUPON_CONDITION_NOT_MET,
                "優惠券無效: " + (calcResult != null ? calcResult.getMessage() : "服務無回應"));
    }

    /** Feign→product-service 扣減一般商品庫存。 */
    private void deductRegularStock(List<DecreaseStockInputDTO> stockList) {
        Result<Void> stockResult = productFeignClient.decreaseStock(stockList);
        if (stockResult == null || !stockResult.isSuccess()) {
            throw new BusinessException(ResultCodeEnum.STOCK_DEDUCT_FAILED,
                    stockResult != null ? stockResult.getMessage() : "庫存服務無回應");
        }
    }

    /** 將優惠券試算結果套用到訂單金額。 */
    private void applyCouponResult(Order order, CouponCalculationResult couponResult) {
        if (couponResult == null)
            return;

        BigDecimal currentDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        order.setDiscountAmount(currentDiscount.add(couponResult.getDiscountAmount()));

        BigDecimal payAmount = order.getTotalAmount()
                .add(order.getShippingFee())
                .subtract(couponResult.getDiscountAmount());
        if (payAmount.compareTo(BigDecimal.ZERO) < 0)
            payAmount = BigDecimal.ZERO;
        order.setPayAmount(payAmount);

        log.info("【異步下單】優惠券套用: orderSn={}, discount={}", order.getOrderSn(), couponResult.getDiscountAmount());
    }

    private void compensate(Long orderId, String orderSn,
            List<DecreaseStockInputDTO> regularStockList, boolean flashSaleDeducted, Throwable cause) {
        // 補償第 1 步：回補一般商品庫存（已扣才需要回補）
        if (regularStockList != null && !regularStockList.isEmpty()) {
            try {
                productFeignClient.increaseStock(regularStockList);
            } catch (Exception ex) {
                log.error("【補償】一般庫存回補失敗，需人工介入: orderSn={}", orderSn, ex);
            }
        }
        // 補償第 2 步：回補特賣庫存（透過 Feign 呼叫 promotion-service）
        if (flashSaleDeducted) {
            try {
                orderTransactionManager.revertFlashSaleStockIfAny(orderSn);
            } catch (Exception ex) {
                log.error("【庫存補償】特賣品回補失敗，需人工介入: orderSn={}", orderSn, ex);
            }
        }
        // 補償第 3 步：將訂單標記為 CREATE_FAILED，前端輪詢會看到失敗結果
        String reason = extractFailReason(cause instanceof Exception ? (Exception) cause : new RuntimeException(cause));
        orderTransactionManager.markOrderFailed(orderId, reason);
    }

    /** 從購物車快照建立計價 Map（信任 enrichWithPricing 已填好價格，零 RPC）。 */
    private Map<Long, ProductPriceResultDTO> buildPricingMapFromCart(List<CartItemDTO> cartItems) {
        Map<Long, ProductPriceResultDTO> fallbackMap = new HashMap<>();
        for (CartItemDTO item : cartItems) {
            ProductPriceResultDTO dto = ProductPriceResultDTO.builder()
                    .variantId(item.getVariantId())
                    .originalPrice(item.getOriginalPrice() != null ? item.getOriginalPrice() : item.getPrice())
                    .finalPrice(item.getPrice())
                    .discountAmount(item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO)
                    .promotionType(item.getPromotionType())
                    .promotionId(item.getPromotionId())
                    .promotionName(item.getPromotionName())
                    .build();
            fallbackMap.put(item.getVariantId(), dto);
        }

        // 向 promotion-service 重算最新價格
        try {
            List<ProductPriceCheckDTO> checks = cartItems.stream()
                    .map(item -> ProductPriceCheckDTO.builder()
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
                            .categoryId(item.getCategoryId())
                            .originalPrice(item.getOriginalPrice() != null ? item.getOriginalPrice() : item.getPrice())
                            .build())
                    .collect(Collectors.toList());

            Result<List<ProductPriceResultDTO>> priceResult = promotionFeignClient.calculateBestPrices(checks);
            if (priceResult != null && priceResult.isSuccess() && priceResult.getData() != null) {
                Map<Long, ProductPriceResultDTO> dynamicMap = priceResult.getData().stream()
                        .filter(p -> p.getVariantId() != null)
                        .collect(Collectors.toMap(ProductPriceResultDTO::getVariantId, p -> p, (a, b) -> a));

                // 補齊缺欄位，避免上游回傳 partial data
                for (Map.Entry<Long, ProductPriceResultDTO> e : dynamicMap.entrySet()) {
                    ProductPriceResultDTO base = fallbackMap.get(e.getKey());
                    if (base == null) {
                        continue;
                    }
                    ProductPriceResultDTO p = e.getValue();
                    if (p.getOriginalPrice() == null) {
                        p.setOriginalPrice(base.getOriginalPrice());
                    }
                    if (p.getFinalPrice() == null) {
                        p.setFinalPrice(base.getFinalPrice());
                    }
                    if (p.getDiscountAmount() == null && p.getOriginalPrice() != null && p.getFinalPrice() != null) {
                        p.setDiscountAmount(p.getOriginalPrice().subtract(p.getFinalPrice()));
                    }
                }
                return dynamicMap;
            }
            log.warn("【異步下單】促銷重算無可用結果，使用購物車快照: orderItems={}", cartItems.size());
        } catch (Exception e) {
            log.warn("【異步下單】促銷重算失敗，回退購物車快照: {}", e.getMessage());
        }
        return fallbackMap;
    }

    /** 重算訂單金額。 */
    private void recalculateOrderAmounts(Order order, List<CartItemDTO> cartItems,
            Map<Long, ProductPriceResultDTO> pricingMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (CartItemDTO item : cartItems) {
            ProductPriceResultDTO priceResult = pricingMap != null ? pricingMap.get(item.getVariantId()) : null;

            BigDecimal price = (priceResult != null && priceResult.getFinalPrice() != null)
                    ? priceResult.getFinalPrice()
                    : item.getPrice();

            BigDecimal originalPrice = (priceResult != null && priceResult.getOriginalPrice() != null)
                    ? priceResult.getOriginalPrice()
                    : (item.getOriginalPrice() != null ? item.getOriginalPrice() : item.getPrice());
            BigDecimal qty = new BigDecimal(item.getQuantity());

            // 累加實際售價 × 數量
            totalAmount = totalAmount.add(price.multiply(qty).setScale(2, java.math.RoundingMode.HALF_UP));

            // 累加折扣額 = (原價 - 售價) × 數量，只統計正數差額
            BigDecimal discount = originalPrice.subtract(price);
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                totalDiscount = totalDiscount.add(discount.multiply(qty).setScale(2, java.math.RoundingMode.HALF_UP));
            }
        }

        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(totalDiscount);
        // payAmount 暫時等於 totalAmount，後續 calculateShipping / applyCoupon 會再調整
        order.setPayAmount(totalAmount);
    }

    private void calculateShipping(Order order, Integer shippingMethod) {
        BigDecimal freeThreshold = shippingProperties.getFreeThreshold();
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal shippingFee;

        // 滿額免運判斷：超過門檻則免運，否則按運送方式收費
        if (totalAmount.compareTo(freeThreshold) >= 0) {
            shippingFee = BigDecimal.ZERO;
        } else {
            // 宅配與超商取貨運費不同，由 ShippingProperties 統一配置
            shippingFee = ShippingMethod.HOME_DELIVERY.getCode().equals(shippingMethod)
                    ? shippingProperties.getDeliveryFee()
                    : shippingProperties.getStoreFee();
        }

        order.setShippingFee(shippingFee);
        order.setPayAmount(totalAmount.add(shippingFee));
    }

    /** 建立一般品庫存扣減清單（排除特賣品，特賣品由 promotion-service 管控）。 */
    private List<DecreaseStockInputDTO> buildRegularStockList(
            List<OrderItem> orderItems, Map<Long, ProductPriceResultDTO> pricingMap) {
        return orderItems.stream()
                // 過濾條件：特賣品的庫存由 flash_sale_skus 管理，不扣主庫存
                .filter(item -> {
                    ProductPriceResultDTO priceResult = pricingMap.get(item.getVariantId());
                    boolean isFlashSale = priceResult != null && "FLASH_SALE".equals(priceResult.getPromotionType());
                    return !isFlashSale;
                })
                .map(item -> {
                    DecreaseStockInputDTO dto = new DecreaseStockInputDTO();
                    dto.setVariantId(item.getVariantId());
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /** 截取異常訊息（≤500 字元）。 */
    private String extractFailReason(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.length() > 500) {
            msg = msg.substring(0, 500);
        }
        return msg != null ? msg : e.getClass().getSimpleName();
    }
}
