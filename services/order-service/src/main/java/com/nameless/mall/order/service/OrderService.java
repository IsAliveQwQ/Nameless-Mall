package com.nameless.mall.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import com.nameless.mall.order.api.dto.OrderSubmitDTO;
import com.nameless.mall.order.entity.Order;
import com.nameless.mall.order.api.vo.OrderDetailVO;
import com.nameless.mall.order.api.vo.OrderVO;

/**
 * 訂單服務的接口
 * <p>
 * 繼承 IService<Order> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 */
public interface OrderService extends IService<Order> {

    /**
     * 提交訂單（異步 Saga 模式）。
     * <p>
     * 同步端僅建立 CREATING(5) 狀態的骨架訂單並立即返回 OrderVO，
     * 核心流程（計價、庫存扣減、明細寫入）由 OrderAsyncProcessor 異步完成。
     * 失敗時透過 Saga 補償回滾庫存，訂單標為 CREATE_FAILED(6)。
     * 前端應透過 {@link #getOrderCreationStatus} 輪詢結果。
     *
     * @param submitDTO 包含所有提交訂單所需資訊的 DTO
     * @return 建立成功的訂單資訊 VO（此時狀態為 CREATING）
     */
    OrderVO submitOrder(Long userId, OrderSubmitDTO submitDTO);

    Page<OrderVO> findPage(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 根據訂單業務編號查詢完整詳情。
     *
     * @param orderSn 訂單業務編號
     * @return 訂單詳情 VO
     */
    OrderDetailVO getOrderDetailBySn(Long userId, String orderSn);

    /** 內部 Feign 介面：根據訂單編號查詢詳情（不檢查用戶權限）。 */
    OrderDetailDTO getOrderDetailInternal(String orderSn);

    /**
     * 產生防重送權杖。
     * 前端進入「訂單確認頁」時呼叫，提交訂單時帶回此 token。
     *
     * @return 唯一防重送權杖
     */
    String generateOrderToken(Long userId);

    /**
     * 取消訂單（補償式流程）。
     * <p>
     * 1. 更新訂單狀態為「已取消」。
     * 2. 透過 Feign 呼叫商品服務回補庫存。
     * 3. 寫入 Outbox 取消消息，由 MessageRelay 投遞 MQ。
     *
     * @param orderSn 要取消的訂單的業務編號
     */
    void cancelOrder(Long userId, String orderSn);

    /**
     * 確認收貨
     * <p>
     * 將訂單狀態從「已出貨」更新為「已完成」
     *
     * @param orderSn 訂單編號
     */
    void confirmReceipt(Long userId, String orderSn);

    /**
     * 處理支付成功回調，更新訂單狀態。
     *
     * @param orderSn 訂單編號
     */
    void handlePaymentSuccess(String orderSn);

    /**
     * 根據訂單編號獲取實體 (內部使用)
     */
    Order getOrderBySn(String orderSn);

    /**
     * 內部取消訂單 (不檢查用戶 Session)
     */
    void cancelOrderInternal(String orderSn);

    /**
     * 查詢訂單建立進度（供前端輪詢用）。
     * 只查主表狀態，不載入 items/shipment，極輕量。
     *
     * @param orderSn 訂單編號
     * @return 訂單基本資訊 VO（含 status, failReason）
     */
    OrderVO getOrderCreationStatus(Long userId, String orderSn);

    /**
     * 創建異步特賣訂單 (僅供 Consumer 調用)
     *
     * @param message 特賣活動消息
     * @return 訂單編號
     */
    String createFlashSaleOrder(com.nameless.mall.order.api.dto.FlashSaleMessage message);
}
