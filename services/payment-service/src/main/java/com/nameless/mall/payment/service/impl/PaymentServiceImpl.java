package com.nameless.mall.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import com.nameless.mall.order.api.feign.OrderFeignClient;
import com.nameless.mall.payment.api.dto.PaymentCreateDTO;
import com.nameless.mall.payment.api.dto.PaymentDTO;
import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentProvider;
import com.nameless.mall.payment.api.enums.PaymentStatus;
import com.nameless.mall.payment.entity.Payment;
import com.nameless.mall.payment.mapper.PaymentMapper;
import com.nameless.mall.payment.mq.PaymentMessageProducer;
import com.nameless.mall.payment.provider.PaymentProviderFactory;
import com.nameless.mall.payment.provider.PaymentProviderStrategy;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;
import com.nameless.mall.payment.provider.dto.PaymentInitContext;
import com.nameless.mall.payment.provider.dto.PaymentInitResult;
import com.nameless.mall.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 支付服務實作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private final PaymentProviderFactory paymentProviderFactory;
    private final OrderFeignClient orderFeignClient;
    private final PaymentMessageProducer paymentMessageProducer;
    private final ObjectMapper objectMapper;

    @Value("${payment.return-url:https://isaliveqwq.me/checkout/callback}")
    private String defaultReturnUrl;

    /**
     * 預設支付方式代碼 (當查無對應 legacy code 時使用)
     */
    private static final int DEFAULT_PAY_METHOD_CODE = 99;

    /**
     * 商品描述前綴
     */
    private static final String PAYMENT_ITEM_NAME_PREFIX = "Nameless Mall Order ";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentDTO createPayment(PaymentCreateDTO dto, Long userId) {
        // 1. 取得現有支付單或初始化
        Payment existingPayment = this.getOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderSn, dto.getOrderSn()));

        if (existingPayment != null && Objects.equals(PaymentStatus.SUCCESS.getCode(), existingPayment.getStatus())) {
            return buildPaymentDTO(existingPayment, null);
        }

        // 2. 驗證並取得訂單資訊 (不依賴前端傳入金額)
        OrderDetailDTO order = validateAndFetchOrder(dto.getOrderSn());
        Long effectiveUserId = userId != null ? userId : order.getUserId();
        if (effectiveUserId == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "無法確認用戶身份");
        }

        // 3. 解析支付方式與獲取策略
        PaymentMethod method = resolvePaymentMethod(dto);
        PaymentProviderStrategy strategy = paymentProviderFactory.getStrategyByMethod(method);

        // 4. 初始化第三方支付
        String paymentSn = generatePaymentSn();
        PaymentInitContext context = PaymentInitContext.builder()
                .paymentSn(paymentSn)
                .orderSn(dto.getOrderSn())
                .userId(effectiveUserId)
                .amount(order.getPayAmount())
                .method(method)
                .itemName(PAYMENT_ITEM_NAME_PREFIX + dto.getOrderSn())
                .returnUrl(defaultReturnUrl)
                .build();

        PaymentInitResult initResult = strategy.initPayment(context);
        if (!initResult.isSuccess()) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_CREATE_FAILED, initResult.getErrorMessage());
        }

        // 5. 儲存或更新支付記錄
        Payment payment = persistPaymentRecord(existingPayment, paymentSn, effectiveUserId, order.getPayAmount(),
                method, initResult, dto, strategy);

        return buildPaymentDTO(payment, initResult);
    }

    /**
     * 驗證訂單是否存在且金額合法
     */
    private OrderDetailDTO validateAndFetchOrder(String orderSn) {
        Result<OrderDetailDTO> orderResult = orderFeignClient.getOrderDetail(orderSn);
        if (orderResult == null || !orderResult.isSuccess() || orderResult.getData() == null) {
            log.error("【支付初始化】查詢訂單失敗: orderSn={}, result={}", orderSn, orderResult);
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND, "找不到對應訂單");
        }

        OrderDetailDTO order = orderResult.getData();
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "訂單金額無效");
        }
        return order;
    }

    /**
     * 解析支付方式 (支援舊版 code 與新版 Enum)
     */
    private PaymentMethod resolvePaymentMethod(PaymentCreateDTO dto) {
        PaymentMethod method = dto.getMethod();
        if (method == null && dto.getPayMethod() != null) {
            method = PaymentMethod.fromLegacyCode(dto.getPayMethod());
        }
        if (method == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "支付方式無效");
        }
        return method;
    }

    /**
     * 建立或更新支付實體並儲存至資料庫
     */
    private Payment persistPaymentRecord(Payment existing, String paymentSn, Long userId, BigDecimal amount,
            PaymentMethod method, PaymentInitResult initResult,
            PaymentCreateDTO dto, PaymentProviderStrategy strategy) {
        Payment payment = existing != null ? existing : new Payment();

        payment.setPaymentSn(paymentSn);
        payment.setOrderSn(dto.getOrderSn());
        payment.setUserId(userId);
        payment.setAmount(amount);

        // 儲存類型與狀態
        Integer legacyCode = method.toLegacyCode();
        payment.setPayMethod(legacyCode != null ? legacyCode : DEFAULT_PAY_METHOD_CODE);
        payment.setStatus(initResult.getStatus().getCode());

        // 儲存提供商資訊
        payment.setProviderName(strategy.getProvider().name());
        payment.setExpiredAt(initResult.getExpireAt());
        payment.setProviderTradeNo(initResult.getProviderTradeNo());

        // 儲存輔助資訊
        payment.setAccountInfo(dto.getAccountInfo());
        payment.setBankCode(dto.getBankCode());
        payment.setBankName(dto.getBankName());
        payment.setPayerName(dto.getPayerName());

        // 時間記錄與重置
        if (payment.getId() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }
        payment.setPaidAt(initResult.getStatus() == PaymentStatus.SUCCESS ? LocalDateTime.now() : null);

        if (payment.getId() != null) {
            this.updateById(payment);
        } else {
            this.save(payment);
        }
        return payment;
    }

    @Override
    public PaymentDTO getPaymentByOrderSn(String orderSn) {
        // 1. 依訂單編號查詢支付記錄
        Payment payment = this.getOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderSn, orderSn));
        // 2. 若查無記錄則回傳 null
        if (payment == null) {
            return null;
        }
        // 3. 轉換為 DTO 並回傳
        return buildPaymentDTO(payment);
    }

    @Override
    public PaymentDTO getPaymentByPaymentSn(String paymentSn) {
        Payment payment = this.getOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentSn, paymentSn));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_FOUND);
        }
        return buildPaymentDTO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(String paymentSn, String accountInfo, Long userId) {
        Payment payment = this.getOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentSn, paymentSn));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_FOUND);
        }

        // 驗證用戶
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ACCESS_DENIED, "無權操作此支付單");
        }

        // 檢查狀態（快速失敗：為使用者提供明確錯誤訊息）
        if (!Objects.equals(PaymentStatus.PENDING.getCode(), payment.getStatus()) &&
                !Objects.equals(PaymentStatus.PROCESSING.getCode(), payment.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_STATUS_INVALID, "支付單狀態不允許確認");
        }

        // CAS 更新：原子地從 PENDING → SUCCESS，防止雙擊/回調+手動確認 TOCTOU 競態
        int rows = baseMapper.update(null,
                new LambdaUpdateWrapper<Payment>()
                        .set(Payment::getStatus, PaymentStatus.SUCCESS.getCode())
                        .set(Payment::getPaidAt, LocalDateTime.now())
                        .set(Payment::getAccountInfo, accountInfo)
                        .eq(Payment::getPaymentSn, paymentSn)
                        .in(Payment::getStatus, PaymentStatus.PENDING.getCode(), PaymentStatus.PROCESSING.getCode()));
        if (rows == 0) {
            log.warn("【CAS 競態保護】支付單已被其他線程處理，忽略重複確認: paymentSn={}", paymentSn);
            return;
        }

        // 手動確認成功後透過 MQ 通知 order-service（在事務 commit 後發送，避免幽靈消息）
        final String orderSnForMQ = payment.getOrderSn();
        final String paymentSnForMQ = payment.getPaymentSn();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        paymentMessageProducer.sendPaymentSuccess(orderSnForMQ, paymentSnForMQ);
                    }
                });

        log.info("【手動支付確認】成功並已註冊 afterCommit 消息: paymentSn={}, orderSn={}", paymentSn, payment.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPayment(String orderSn) {
        // CAS 取消：僅在 PENDING 狀態下才允許取消，防止覆蓋第三方已成功的支付
        int rows = baseMapper.update(null,
                new LambdaUpdateWrapper<Payment>()
                        .set(Payment::getStatus, PaymentStatus.CANCELLED.getCode())
                        .eq(Payment::getOrderSn, orderSn)
                        .in(Payment::getStatus, PaymentStatus.PENDING.getCode(), PaymentStatus.PROCESSING.getCode()));
        if (rows == 0) {
            log.warn("【支付取消】支付單非 PENDING 狀態，跳過取消: orderSn={}", orderSn);
        }
    }

    /**
     * 生成支付單編號
     */
    private String generatePaymentSn() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentCallbackResult handleCallback(String providerName, Map<String, String> params) {
        PaymentProviderStrategy strategy;
        try {
            PaymentProvider provider = PaymentProvider.valueOf(providerName.toUpperCase());
            strategy = paymentProviderFactory.getStrategy(provider);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "不支援的金流提供商: " + providerName);
        }

        log.info("【金流回調】開始處理第三方回調. 參數總數: {}", params.size());
        try {
            log.debug("【金流回調】原始參數清單: {}", objectMapper.writeValueAsString(params));
        } catch (Exception e) {
            log.warn("【金流回調】參數 JSON 序列化失敗");
        }

        // 1. 查詢支付單（支援多種識別碼）
        Payment payment = findPaymentByCallbackParams(params);

        if (Objects.equals(PaymentStatus.SUCCESS.getCode(), payment.getStatus())) {
            log.warn("【金流回調警告】支付單已為成功狀態，忽略重複回調: paymentSn={}", payment.getPaymentSn());
            return PaymentCallbackResult.builder()
                    .success(true)
                    .status(PaymentStatus.SUCCESS)
                    .providerTradeNo(payment.getProviderTradeNo())
                    .paidAt(payment.getPaidAt())
                    .build();
        }

        // 補齊金額參數，供 LINE Pay confirm 等 Provider 使用
        if (!params.containsKey("amount")) {
            params.put("amount", payment.getAmount().setScale(0, RoundingMode.HALF_UP).toString());
        }

        // 2. 執行回調處理 (呼叫第三方驗證)
        PaymentCallbackResult result = strategy.handleCallback(params);

        if (!result.isSuccess()) {
            log.error("【金流回調失敗】第三方驗證不通過: {}, 原始參數: {}", result.getErrorMessage(), params);
            throw new BusinessException(ResultCodeEnum.PAYMENT_FAILED,
                    String.format("[%s] %s", providerName, result.getErrorMessage()));
        }

        // 3. CAS 更新支付單狀態：防止回調與手動確認 TOCTOU 競態
        if (PaymentStatus.SUCCESS.equals(result.getStatus())) {
            LocalDateTime paidAt = result.getPaidAt() != null ? result.getPaidAt() : LocalDateTime.now();
            int rows = baseMapper.update(null,
                    new LambdaUpdateWrapper<Payment>()
                            .set(Payment::getStatus, PaymentStatus.SUCCESS.getCode())
                            .set(Payment::getPaidAt, paidAt)
                            .set(Payment::getProviderResponse, result.getProviderResponse())
                            .eq(Payment::getId, payment.getId())
                            .in(Payment::getStatus, PaymentStatus.PENDING.getCode(),
                                    PaymentStatus.PROCESSING.getCode()));
            if (rows == 0) {
                log.warn("【CAS 競態保護】支付單已被處理，忽略重複回調: paymentSn={}", payment.getPaymentSn());
                return PaymentCallbackResult.builder()
                        .success(true)
                        .status(PaymentStatus.SUCCESS)
                        .providerTradeNo(payment.getProviderTradeNo())
                        .paidAt(paidAt)
                        .build();
            }

            // 透過 MQ 通知 order-service 更新訂單狀態為已支付（事務 commit 後發送，避免幽靈消息）
            final String orderSnForMQ = payment.getOrderSn();
            final String paymentSnForMQ = payment.getPaymentSn();
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            paymentMessageProducer.sendPaymentSuccess(orderSnForMQ, paymentSnForMQ);
                        }
                    });

            log.info("支付成功並已註冊 afterCommit 消息: paymentSn={}, method={}", payment.getPaymentSn(), providerName);
        } else {
            // 處理失敗或取消狀態（CAS：僅從 PENDING 轉移，防止覆蓋已成功的狀態）
            if (result.getStatus() != null) {
                baseMapper.update(null,
                        new LambdaUpdateWrapper<Payment>()
                                .set(Payment::getStatus, result.getStatus().getCode())
                                .eq(Payment::getId, payment.getId())
                                .in(Payment::getStatus, PaymentStatus.PENDING.getCode(),
                                        PaymentStatus.PROCESSING.getCode()));
            }
        }

        return result;
    }

    /**
     * 構建支付 DTO
     */
    private PaymentDTO buildPaymentDTO(Payment payment, PaymentInitResult initResult) {
        // 1. 複製基礎欄位至 DTO
        PaymentDTO dto = new PaymentDTO();
        BeanUtils.copyProperties(payment, dto);

        // 2. 轉換支付方式與狀態為 Enum 表示
        dto.setMethod(PaymentMethod.fromLegacyCode(payment.getPayMethod()));
        dto.setPaymentStatus(PaymentStatus.fromCode(payment.getStatus()));

        // 3. 若有第三方初始化結果，填入跳轉與表單資訊
        if (initResult != null) {
            dto.setRedirectUrl(initResult.getRedirectUrl());
            if (initResult.getRedirectType() != null) {
                dto.setRedirectType(initResult.getRedirectType().name());
            }
            dto.setFormData(initResult.getFormData());
        }

        return dto;
    }

    // 兼容舊方法
    private PaymentDTO buildPaymentDTO(Payment payment) {
        return buildPaymentDTO(payment, null);
    }

    /**
     * 根據回調參數查詢支付單。
     * 
     * 支援多種識別碼：paymentSn > orderId > MerchantTradeNo > transactionId
     * 
     * @param params 回調參數
     * @return 支付單實體
     * @throws BusinessException 若找不到對應的支付單
     */
    private Payment findPaymentByCallbackParams(Map<String, String> params) {
        // 嘗試從參數解析識別碼
        String paymentSn = params.get("paymentSn"); // 優先使用回調帶回的 paymentSn
        if (paymentSn == null) {
            paymentSn = params.get("orderId"); // 次選 LINE Pay 官方可能帶的 orderId
        }
        if (paymentSn == null) {
            paymentSn = params.get("MerchantTradeNo"); // ECPay 使用 MerchantTradeNo
        }
        String transactionId = params.get("transactionId");

        Payment payment = null;
        if (paymentSn != null) {
            payment = this.getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentSn, paymentSn));
        }

        // 若透過 paymentSn 找不到，嘗試透過第三方交易號查詢
        if (payment == null && transactionId != null) {
            payment = this.getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getProviderTradeNo, transactionId));
        }

        if (payment == null) {
            log.error("【金流回調錯誤】找不到對應的支付單: paymentSn={}, transactionId={}", paymentSn, transactionId);
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_FOUND);
        }

        return payment;
    }
}
