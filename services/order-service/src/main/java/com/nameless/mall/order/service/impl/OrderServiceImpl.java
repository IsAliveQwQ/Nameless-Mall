package com.nameless.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.cart.api.feign.CartFeignClient;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.coupon.api.feign.CouponFeignClient;
import com.nameless.mall.order.api.dto.FlashSaleMessage;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import com.nameless.mall.order.api.dto.OrderSubmitDTO;
import com.nameless.mall.order.api.enums.OrderStatus;
import com.nameless.mall.order.api.enums.ShippingMethod;
import com.nameless.mall.order.api.vo.OrderDetailVO;
import com.nameless.mall.order.api.vo.OrderVO;
import com.nameless.mall.order.entity.Order;
import com.nameless.mall.order.entity.OrderItem;
import com.nameless.mall.order.entity.OrderShipment;
import com.nameless.mall.order.mapper.OrderMapper;
import com.nameless.mall.order.mq.OrderMessageProducer;
import com.nameless.mall.order.service.OrderItemService;
import com.nameless.mall.order.service.OrderService;
import com.nameless.mall.order.service.OrderShipmentService;
import com.nameless.mall.order.service.ReliableMessageService;
import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.core.domain.Result;
import org.springframework.beans.BeanUtils;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.feign.ProductFeignClient;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;

import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.promotion.api.feign.PromotionFeignClient;
import com.nameless.mall.payment.api.feign.PaymentFeignClient;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import com.nameless.mall.order.component.OrderAsyncProcessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 訂單服務的實現類
 * <p>
 * 此類專注於 Order 實體的業務邏輯。
 * OrderItem 相關邏輯由 OrderItemService 處理。
 * OrderShipment 相關邏輯由 OrderShipmentService 處理。
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemService orderItemService;
    private final OrderShipmentService orderShipmentService;
    private final PaymentFeignClient paymentFeignClient;
    private final StringRedisTemplate redisTemplate;
    private final ProductFeignClient productFeignClient;
    private final OrderMessageProducer orderMessageProducer;
    private final CouponFeignClient couponFeignClient;
    private final PromotionFeignClient promotionFeignClient;
    private final TransactionTemplate transactionTemplate;
    private final ReliableMessageService reliableMessageService;
    private final CartFeignClient cartFeignClient;
    private final OrderAsyncProcessor orderAsyncProcessor;
    private final Executor feignCallExecutor;

    public OrderServiceImpl(
            OrderItemService orderItemService,
            OrderShipmentService orderShipmentService,
            PaymentFeignClient paymentFeignClient,
            StringRedisTemplate redisTemplate,
            ProductFeignClient productFeignClient,
            OrderMessageProducer orderMessageProducer,
            CouponFeignClient couponFeignClient,
            PromotionFeignClient promotionFeignClient,
            TransactionTemplate transactionTemplate,
            ReliableMessageService reliableMessageService,
            CartFeignClient cartFeignClient,
            OrderAsyncProcessor orderAsyncProcessor,
            @Qualifier("feignCallExecutor") Executor feignCallExecutor) {
        this.orderItemService = orderItemService;
        this.orderShipmentService = orderShipmentService;
        this.paymentFeignClient = paymentFeignClient;
        this.redisTemplate = redisTemplate;
        this.productFeignClient = productFeignClient;
        this.orderMessageProducer = orderMessageProducer;
        this.couponFeignClient = couponFeignClient;
        this.promotionFeignClient = promotionFeignClient;
        this.transactionTemplate = transactionTemplate;
        this.reliableMessageService = reliableMessageService;
        this.cartFeignClient = cartFeignClient;
        this.orderAsyncProcessor = orderAsyncProcessor;
        this.feignCallExecutor = feignCallExecutor;
    }

    private static final String ORDER_TOKEN_PREFIX = "mall:order:token:";

    /**
     * 異步下單：同步建骨架 → 觸發異步處理 → 前端輪詢等待完成。
     */
    @Override
    public OrderVO submitOrder(Long userId, OrderSubmitDTO submitDTO) {
        String orderSn = UUID.randomUUID().toString().replace("-", "");

        verifyOrderToken(userId, submitDTO.getOrderToken());
        List<CartItemDTO> cartItems = fetchCheckedCartItems(submitDTO.getCartItemIds());

        // 防重複下單：5 分鐘內有 CREATING 訂單則復用
        Order existingCreating = this.getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, OrderStatus.CREATING.getCode())
                .gt(Order::getCreatedAt, LocalDateTime.now().minusMinutes(5)));
        if (existingCreating != null) {
            log.info("【下單】偵測到進行中訂單，復用: orderSn={}", existingCreating.getOrderSn());
            return buildOrderVO(existingCreating);
        }

        Order order = buildOrder(userId, submitDTO, cartItems, orderSn);
        this.save(order);

        orderAsyncProcessor.processOrderAsync(
                order.getId(), orderSn, userId, submitDTO, cartItems);

        return buildOrderVO(order);
    }

    /** 驗證並消費 Token（Redis DEL 原子防重）。 */
    private void verifyOrderToken(Long userId, String orderToken) {
        String redisKey = ORDER_TOKEN_PREFIX + userId + ":" + orderToken;
        Boolean deleted = redisTemplate.delete(redisKey);
        if (deleted == null || !deleted) {
            throw new BusinessException(ResultCodeEnum.ORDER_DUPLICATE, "請勿重複提交訂單或頁面已過期");
        }
    }

    private List<CartItemDTO> cartItemsFromIds(List<Long> ids) {
        Result<List<CartItemDTO>> cartResult = cartFeignClient.getCartItemsByIds(ids);
        if (cartResult == null || !cartResult.isSuccess() || cartResult.getData() == null) {
            throw new BusinessException(ResultCodeEnum.SERVICE_UNAVAILABLE, "購物車服務暫時不可用");
        }
        return cartResult.getData();
    }

    private List<CartItemDTO> fetchCheckedCartItems(List<Long> ids) {
        // 透過 Feign 拉取購物車已勾選的商品項目
        List<CartItemDTO> items = cartItemsFromIds(ids);
        // 空車防禦：沒有勾選任何商品則拒絕下單
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException(ResultCodeEnum.CART_EMPTY, "沒有找到要結帳的商品");
        }
        return items;
    }

    @Override
    public Order getOrderBySn(String orderSn) {
        return this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderSn, orderSn));
    }

    @Override
    public void cancelOrder(Long userId, String orderSn) {
        // 透過 orderSn + userId 雙重條件查詢，防止越權操作
        Order order = this.getOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderSn, orderSn)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND, "訂單不存在或無權訪問");
        }

        // 委派給內部取消方法（MQ 超時自動取消也走同一入口）
        this.cancelOrderInternal(orderSn);
    }

    /**
     * 訂單內部取消邏輯。
     * 
     * 設計：DB 事務內零 Feign，避免 Feign 延遲佔占 HikariCP 連線。
     * 流程：查詢驗證 → 狀態更新(DB事務) → 庫存返還+支付取消+優惠券退還(並行Feign,事務外)
     */
    @Override
    public void cancelOrderInternal(String orderSn) {
        // 1. 查詢並驗證訂單狀態
        Order order = findOrderForCancellation(orderSn);
        if (order == null) {
            return;
        }

        // 2. DB 事務：原子更新狀態 + 放棄待發送優惠券消息 + 發送取消 Outbox
        boolean updated = cancelOrderDB(order, orderSn);
        if (!updated) {
            return;
        }

        // 3. 事務外並行 Feign 補償：庫存返還 + 支付取消 + 優惠券退還
        CompletableFuture<Void> stockFuture = CompletableFuture.runAsync(
                () -> revertInventory(order, orderSn), feignCallExecutor);
        CompletableFuture<Void> paymentFuture = CompletableFuture.runAsync(
                () -> cancelPaymentRecord(orderSn), feignCallExecutor);
        CompletableFuture<Void> couponFuture = CompletableFuture.runAsync(
                () -> revertCouponIfUsed(order, orderSn), feignCallExecutor);

        try {
            // 庫存和支付是關鍵補償，必須等待結果
            CompletableFuture.allOf(stockFuture, paymentFuture).join();
        } catch (Exception e) {
            log.error("【訂單取消】關鍵補償失敗 (庫存/支付): orderSn={}", orderSn, e);
            throw new BusinessException(ResultCodeEnum.STOCK_RETURN_FAILED,
                    "訂單取消補償失敗，請稍後重試");
        }

        // 優惠券容錯，不阻斷
        try {
            couponFuture.join();
        } catch (Exception e) {
            log.warn("【訂單取消】優惠券退還失敗（不阻斷）: orderSn={}", orderSn, e);
        }
    }

    /**
     * 取消訂單的 DB 事務：僅包含純 DB 操作，零 Feign 呼叫。
     * <p>
     * CAS 更新狀態 + 放棄待發送優惠券消息 + Outbox 發送取消事件。
     * <p>
     * 使用 TransactionTemplate 程式化事務管理，避免 self-invocation 導致
     * {@code @Transactional} AOP 攔截失效的問題。
     *
     * @return true 如果狀態更新成功，false 如果已被其他線程處理
     */
    private boolean cancelOrderDB(Order order, String orderSn) {
        Boolean result = transactionTemplate.execute(status -> {
            boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                    .eq(Order::getOrderSn, orderSn)
                    .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                    .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
                    .set(Order::getUpdatedAt, LocalDateTime.now()));

            if (!updated) {
                log.warn("【訂單取消】失敗：狀態已被變更或不符，攔截重複處理。orderSn={}", orderSn);
                return false;
            }

            // 放棄 Outbox 中未發送的優惠券核銷消息
            reliableMessageService.killPendingCouponMessage(orderSn);

            // Transactional Outbox 發送取消消息（Promotion Service 會釋放特賣庫存）
            orderMessageProducer.sendOrderCancelled(orderSn);

            return true;
        });
        return Boolean.TRUE.equals(result);
    }

    /**
     * 查詢訂單並驗證是否可取消。
     * 
     * @return 訂單實體，若不存在或狀態不符則返回 null
     */
    private Order findOrderForCancellation(String orderSn) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderSn, orderSn));
        if (order == null) {
            log.warn("【訂單取消】找不到訂單，忽略取消請求: orderSn={}", orderSn);
            return null;
        }

        if (!OrderStatus.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
            log.info("【訂單取消】狀態不符 (status={})，跳過取消: orderSn={}",
                    order.getStatus(), orderSn);
            return null; // 已支付或已取消，無需處理
        }

        return order;
    }

    /**
     * 返還一般庫存。
     * 
     * 注意：失敗會阻斷取消流程（庫存不一致會導致超賣）
     */
    private void revertInventory(Order order, String orderSn) {
        // 1. 查詢訂單包含的所有商品項目
        List<OrderItem> orderItems = orderItemService.getByOrderId(order.getId());
        if (CollectionUtils.isEmpty(orderItems)) {
            return;
        }

        // 2. 過濾特賣品 — 特賣庫存由 promotion-service 管理，此處只回退一般庫存
        List<Long> flashSaleSkuIds = baseMapper.selectFlashSaleSkuIds(orderSn);
        List<OrderItem> regularItems = orderItems;
        if (!CollectionUtils.isEmpty(flashSaleSkuIds)) {
            regularItems = orderItems.stream()
                    .filter(item -> !flashSaleSkuIds.contains(item.getVariantId()))
                    .collect(Collectors.toList());
            log.info("【庫存返還】過濾特賣品: orderSn={}, flashSaleSkus={}, regularCount={}",
                    orderSn, flashSaleSkuIds.size(), regularItems.size());
        }

        if (regularItems.isEmpty()) {
            return;
        }

        // 3. 組裝庫存回補 DTO，透過 Feign 呼叫 product-service 增加庫存
        List<DecreaseStockInputDTO> stockToIncrease = orderItemService.buildStockDTOList(regularItems);
        Result<Void> stockResult = productFeignClient.increaseStock(stockToIncrease);
        if (stockResult == null || !stockResult.isSuccess()) {
            log.error("【庫存返還失敗】orderSn={}", orderSn);
            throw new BusinessException(ResultCodeEnum.STOCK_RETURN_FAILED, "庫存返還失敗");
        }
    }

    /**
     * 取消支付紀錄。
     * 
     * 注意：失敗會阻斷取消流程（支付不一致會導致重複收款）
     */
    private void cancelPaymentRecord(String orderSn) {
        try {
            paymentFeignClient.cancelPayment(orderSn);
        } catch (Exception e) {
            log.error("【支付服務連動取消失敗】orderSn={}, error={}", orderSn, e.getMessage());
            throw new BusinessException(ResultCodeEnum.PAYMENT_CANCEL_FAILED, "支付單取消失敗，請稍後再試");
        }
    }

    /**
     * 若訂單有使用優惠券，則退還優惠券。
     * 
     * 注意：失敗不阻斷取消流程（可人工補償）
     */
    private void revertCouponIfUsed(Order order, String orderSn) {
        if (order.getUserCouponId() == null) {
            return;
        }

        Result<Void> couponResult = couponFeignClient.returnCoupon(order.getUserCouponId());
        if (couponResult == null || !couponResult.isSuccess()) {
            log.warn("【優惠券退還失敗】orderSn={}, couponId={} - 記錄但不阻斷取消流程",
                    orderSn, order.getUserCouponId());
        }
    }

    @Override
    public Page<OrderVO> findPage(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        // 1. 組裝查詢條件：限定當前使用者 + 可選狀態篩選 + 按建立時間倒序
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> orderPage = this.baseMapper.selectPage(page, queryWrapper);

        // 2. Entity → VO 轉換，並封裝為分頁結果返回
        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(this::buildOrderVO)
                .collect(Collectors.toList());
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public OrderDetailVO getOrderDetailBySn(Long userId, String orderSn) {
        // 透過 orderSn + userId 查詢，確保使用者只能看到自己的訂單
        Order order = this.getOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderSn, orderSn)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND, "訂單不存在或無權訪問");
        }
        // 組裝完整訂單詳情（含明細項、物流資訊）
        return buildOrderDetailVO(order);
    }

    @Override
    public OrderDetailDTO getOrderDetailInternal(String orderSn) {
        // 內部呼叫不檢查用戶 ID，改由 Gateway 防禦 /internal/** 路徑
        Order order = this.getOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderSn, orderSn));
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        // 查詢關聯的訂單項目和物流資訊，組裝為內部 DTO 返回
        List<OrderItem> orderItems = orderItemService.getByOrderId(order.getId());
        OrderShipment shipment = orderShipmentService.getByOrderId(order.getId());
        return buildOrderDetailDTO(order, orderItems, shipment);
    }

    private OrderDetailVO buildOrderDetailVO(Order order) {
        OrderDetailVO vo = new OrderDetailVO();
        // 複製同名屬性
        BeanUtils.copyProperties(order, vo);

        // 2. 設置衍生屬性
        vo.setStatusName(getStatusName(order.getStatus()));
        vo.setPayTypeName(getPayTypeName(order.getPayType()));

        String shippingMethodName = "未知";
        if (ShippingMethod.HOME_DELIVERY.getCode().equals(order.getShippingMethod())) {
            shippingMethodName = ShippingMethod.HOME_DELIVERY.getDescription();
        } else if (ShippingMethod.CONVENIENCE_STORE.getCode().equals(order.getShippingMethod())) {
            shippingMethodName = ShippingMethod.CONVENIENCE_STORE.getDescription();
        }
        vo.setShippingMethodName(shippingMethodName);

        // 3. 處理關聯資料
        List<OrderItem> orderItems = orderItemService.getByOrderId(order.getId());
        if (!CollectionUtils.isEmpty(orderItems)) {
            vo.setItems(orderItems.stream().map(item -> {
                OrderDetailVO.OrderItemVO itemVO = new OrderDetailVO.OrderItemVO();
                BeanUtils.copyProperties(item, itemVO); // Item 屬性通常一致
                itemVO.setProductVariantName(item.getSkuName() != null ? item.getSkuName() : "");
                return itemVO;
            }).collect(Collectors.toList()));
        }

        OrderShipment shipment = orderShipmentService.getByOrderId(order.getId());
        if (shipment != null) {
            OrderDetailVO.ShipmentVO shipmentVO = new OrderDetailVO.ShipmentVO();
            BeanUtils.copyProperties(shipment, shipmentVO);
            vo.setShipment(shipmentVO);
        }

        return vo;
    }

    private String getStatusName(Integer status) {
        if (status == null)
            return "未知";
        for (OrderStatus s : OrderStatus.values()) {
            if (s.getCode().equals(status))
                return s.getDescription();
        }
        return "未知狀態";
    }

    private String getPayTypeName(Integer payType) {
        if (payType == null)
            return "未指定";
        PaymentMethod method = PaymentMethod.fromLegacyCode(payType);
        return method != null ? method.getDisplayName() : "其他支付";
    }

    @Override
    public String generateOrderToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = ORDER_TOKEN_PREFIX + userId + ":" + token;
        redisTemplate.opsForValue().set(redisKey, "1", 30, TimeUnit.MINUTES);
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long userId, String orderSn) {
        // 1. 驗證身份與訂單存在性
        Order order = this.getOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderSn, orderSn)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND, "訂單不存在或無權訪問");
        }

        // 2. 狀態防禦：只有「已出貨」才能確認收貨
        if (!OrderStatus.SHIPPED.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_INVALID, "訂單狀態不符，無法確認收貨");
        }

        // 3. 更新訂單主表狀態為「已完成」
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        this.updateById(order);

        // 4. 更新物流表的簽收時間
        orderShipmentService.confirmReceived(order.getId());
    }

    @Override
    public void handlePaymentSuccess(String orderSn) {
        // CAS 原子更新：僅在狀態為「待付款」時更新為「處理中」，防止重複回調競態
        int rows = baseMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .set(Order::getStatus, OrderStatus.PROCESSING.getCode())
                        .set(Order::getPaidAt, LocalDateTime.now())
                        .set(Order::getUpdatedAt, LocalDateTime.now())
                        .eq(Order::getOrderSn, orderSn)
                        .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.getCode()));

        if (rows > 0) {
            log.info("【支付回調】訂單狀態更新成功: orderSn={}, status={}", orderSn, OrderStatus.PROCESSING.getDescription());
        } else {
            log.warn("【支付回調】訂單不存在或狀態非待付款，跳過更新: orderSn={}", orderSn);
        }
    }

    private OrderDetailDTO buildOrderDetailDTO(Order order, List<OrderItem> items, OrderShipment shipment) {
        OrderDetailDTO detailDTO = new OrderDetailDTO();
        // 自動複製同名屬性
        BeanUtils.copyProperties(order, detailDTO);

        // 透過 Service 轉換關聯資料
        detailDTO.setShipment(orderShipmentService.toDTO(shipment));
        detailDTO.setItems(orderItemService.toDTO(items));

        return detailDTO;
    }

    /** 建立 CREATING 訂單骨架，金額為估算值（異步階段精算覆蓋）。 */
    private Order buildOrder(Long userId, OrderSubmitDTO submitDTO, List<CartItemDTO> cartItems, String orderSn) {
        // 1. 填入基本欄位：編號、使用者、支付方式、運送方式
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setPayType(submitDTO.getPayType());
        order.setShippingMethod(submitDTO.getShippingMethod());
        order.setPaymentAccountInfo(submitDTO.getPaymentAccountInfo());
        order.setStatus(OrderStatus.CREATING.getCode());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setUserCouponId(submitDTO.getUserCouponId());
        order.setNote(submitDTO.getNote());

        // 2. 粗估總金額（以購物車單價 × 數量加總，異步階段會用促銷價覆蓋）
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity()))
                        .setScale(2, java.math.RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 3. 暫定金額（運費、折扣在異步階段 calculateShipping / applyCoupon 覆蓋）
        order.setShippingFee(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);

        return order;
    }

    /** 查詢訂單建立進度（前端輪詢用，僅查主表）。 */
    @Override
    public OrderVO getOrderCreationStatus(Long userId, String orderSn) {
        Order order = this.getOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderSn, orderSn)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND, "訂單不存在");
        }
        return buildOrderVO(order);
    }

    private OrderVO buildOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        // 複製同名屬性
        BeanUtils.copyProperties(order, vo);

        // 設置衍生屬性 (需手動轉換)
        vo.setStatusName(getStatusName(order.getStatus()));
        vo.setPayTypeName(getPayTypeName(order.getPayType()));

        return vo;
    }

    @Override
    public String createFlashSaleOrder(FlashSaleMessage message) {
        log.info("【異步下單】開始處理特賣訂單: userId={}, skuId={}", message.getUserId(), message.getSkuId());

        String orderSn = UUID.randomUUID().toString().replace("-", "");

        // 1. 查詢商品資訊
        VariantDTO variant = null;
        try {
            Result<List<VariantDTO>> variantResult = productFeignClient
                    .getVariantsBatch(Collections.singletonList(message.getSkuId()));
            if (variantResult != null && variantResult.isSuccess()
                    && !CollectionUtils.isEmpty(variantResult.getData())) {
                variant = variantResult.getData().get(0);
            }
        } catch (Exception e) {
            log.error("【異步下單】查詢商品失敗: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.SERVICE_UNAVAILABLE, "無法獲取商品資訊");
        }

        if (variant == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND, "商品不存在");
        }

        // 2. 查特賣價
        ProductPriceCheckDTO check = ProductPriceCheckDTO.builder()
                .productId(variant.getProductId())
                .variantId(variant.getId())
                .categoryId(variant.getCategoryId())
                .originalPrice(variant.getPrice())
                .build();

        BigDecimal finalPrice = variant.getPrice();
        String promotionName = null;

        try {
            Result<List<ProductPriceResultDTO>> priceResult = promotionFeignClient
                    .calculateBestPrices(Collections.singletonList(check));
            if (priceResult != null && priceResult.isSuccess() && !CollectionUtils.isEmpty(priceResult.getData())) {
                ProductPriceResultDTO priceData = priceResult.getData().get(0);
                if ("FLASH_SALE".equals(priceData.getPromotionType())) {
                    finalPrice = priceData.getFinalPrice();
                    promotionName = priceData.getPromotionName();
                }
            }
        } catch (Exception e) {
            log.error("【異步下單】特賣計價服務異常，無法確認價格: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.SERVICE_UNAVAILABLE, "特賣價格計算失敗，請稍後重試");
        }

        // 3. 構建 Order 對象
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setUserId(message.getUserId());
        order.setTotalAmount(finalPrice.multiply(BigDecimal.valueOf(message.getQuantity())));
        order.setPayAmount(finalPrice.multiply(BigDecimal.valueOf(message.getQuantity())));
        order.setPayType(message.getPayType());
        order.setShippingMethod(message.getShippingMethod());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setShippingFee(BigDecimal.ZERO); // 特賣免運
        BigDecimal discountUnit = variant.getPrice().subtract(finalPrice);
        if (discountUnit.compareTo(BigDecimal.ZERO) < 0)
            discountUnit = BigDecimal.ZERO;
        order.setDiscountAmount(discountUnit.multiply(BigDecimal.valueOf(message.getQuantity())));

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 4. 準備 OrderItem 物件
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(null);
        orderItem.setOrderSn(orderSn);
        orderItem.setProductId(variant.getProductId());
        orderItem.setVariantId(variant.getId());
        orderItem.setProductName(variant.getProductName());
        orderItem.setSkuName(variant.getName());
        orderItem.setProductImage(variant.getImage());
        orderItem.setProductPrice(finalPrice);
        orderItem.setOriginalPrice(variant.getPrice());
        orderItem.setQuantity(message.getQuantity());
        orderItem.setPromotionName(promotionName);
        orderItem.setPromotionAmount(discountUnit.multiply(BigDecimal.valueOf(message.getQuantity())));

        // 5. 準備 Shipment 物件
        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(null);
        shipment.setOrderSn(orderSn);
        shipment.setReceiverName(message.getReceiverName());
        shipment.setReceiverPhone(message.getReceiverPhone());
        shipment.setReceiverAddress(message.getReceiverAddress());

        // 6. 寫入 DB（TransactionTemplate 控制事務範圍，避免長事務）
        transactionTemplate.execute(status -> {
            try {
                this.save(order);

                orderItem.setOrderId(order.getId());
                orderItemService.save(orderItem);

                shipment.setOrderId(order.getId());
                orderShipmentService.save(shipment);

                // 特賣訂單不扣主庫存（庫存已在活動上架時劃撥至 flash_sale_skus）

                // 寫入特賣記錄表，唯一約束 (userId + promotionId + skuId) 確保冪等與限購
                ((OrderMapper) this.baseMapper).insertFlashSaleRecord(
                        message.getUserId(),
                        message.getPromotionId(),
                        message.getSkuId(),
                        orderSn,
                        message.getQuantity());

                // 寫入訂單延遲取消任務 (Outbox 模式，與一般訂單一致)
                reliableMessageService.createOrderDelayMessage(orderSn);

            } catch (Exception e) {
                status.setRollbackOnly();

                // 處理唯一鍵衝突 (重複購買)
                if (e instanceof org.springframework.dao.DuplicateKeyException ||
                        (e.getCause() instanceof org.springframework.dao.DuplicateKeyException)) {
                    log.warn("【異步下單】重複請求攔截 (DB視角): orderSn={}, error={}", orderSn, e.getMessage());
                    throw new BusinessException(ResultCodeEnum.PROMOTION_ALREADY_PARTICIPATED, "您已參與過此特賣活動");
                }

                // 其他未知錯誤
                log.error("【異步下單】寫入 DB 失敗: orderSn={}", orderSn, e);
                throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "訂單寫入失敗");
            }
            return null;
        });

        log.info("【異步下單】成功寫入 DB: orderSn={}", orderSn);
        return orderSn;
    }
}
