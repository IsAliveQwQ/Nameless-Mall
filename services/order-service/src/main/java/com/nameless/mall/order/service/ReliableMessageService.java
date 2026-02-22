package com.nameless.mall.order.service;

import com.nameless.mall.order.entity.LocalMessage;

/**
 * 可靠訊息服務
 * 負責將消息寫入本地訊息表。
 */
public interface ReliableMessageService {

    /**
     * 創建並保存一則「訂單取消」的本地消息。
     * 此方法必須在訂單取消的業務事務中執行。
     * 
     * @param orderSn 訂單編號
     */
    void createOrderCancelledMessage(String orderSn);

    /**
     * 更新消息狀態為已發送
     * 
     * @param messageId 消息 ID
     */
    void markAsSent(String messageId);

    /**
     * 標記消息發送失敗 (並排程重試)
     * 
     * @param message 消息實體
     */
    void markAsFailed(LocalMessage message);

    /**
     * 創建並保存一則「優惠券核銷」的本地消息。
     * 此方法必須在下單的業務事務中執行，確保原子性。
     *
     * @param userCouponId 使用者優惠券 ID
     * @param orderSn      訂單編號
     */
    void createCouponUseMessage(Long userCouponId, String orderSn);

    /**
     * 殺掉尚未發送的優惠券核銷消息（訂單取消時使用）。
     * 防止取消後優惠券仍被異步核銷。
     *
     * @param orderSn 訂單編號
     */
    void killPendingCouponMessage(String orderSn);

    /**
     * 創建並保存一則「訂單建立」的本地消息。
     * 
     * @param orderId    訂單 ID
     * @param productIds 訂單包含的商品 ID 列表
     */
    void createOrderCreatedMessage(Long orderId, java.util.List<Long> productIds);

    /**
     * 創建並保存一則「訂單延遲取消」的本地消息。
     * 
     * @param orderSn 訂單編號
     */
    void createOrderDelayMessage(String orderSn);
}
