package com.nameless.mall.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.payment.api.dto.PaymentCreateDTO;
import com.nameless.mall.payment.api.dto.PaymentDTO;
import com.nameless.mall.payment.entity.Payment;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;
import java.util.Map;

/**
 * 支付服務介面
 */
public interface PaymentService extends IService<Payment> {

    /**
     * 創建支付單
     * 
     * @param dto    支付創建 DTO
     * @param userId 用戶 ID
     * @return 支付單 DTO
     */
    PaymentDTO createPayment(PaymentCreateDTO dto, Long userId);

    /**
     * 根據訂單編號查詢支付單
     * 
     * @param orderSn 訂單編號
     * @return 支付單 DTO
     */
    PaymentDTO getPaymentByOrderSn(String orderSn);

    /**
     * 根據支付單編號查詢支付單
     * 
     * @param paymentSn 支付單編號
     * @return 支付單 DTO
     */
    PaymentDTO getPaymentByPaymentSn(String paymentSn);

    /**
     * 確認付款（銀行轉帳）
     * 
     * @param paymentSn   支付單編號
     * @param accountInfo 帳戶資訊（後五碼）
     * @param userId      用戶 ID
     */
    void confirmPayment(String paymentSn, String accountInfo, Long userId);

    /**
     * 取消支付單（訂單取消時呼叫）
     * 
     * @param orderSn 訂單編號
     */
    void cancelPayment(String orderSn);

    /**
     * 處理第三方支付回調 (包含 LINE Pay 確認)
     * 
     * @param providerName 提供商名稱 (ECPAY, LINEPAY)
     * @param params       回調參數
     * @return 處理結果
     */
    PaymentCallbackResult handleCallback(String providerName, Map<String, String> params);
}
