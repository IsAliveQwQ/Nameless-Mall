package com.nameless.mall.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import com.nameless.mall.order.api.dto.OrderSubmitDTO;
import com.nameless.mall.order.service.OrderService;
import com.nameless.mall.order.api.vo.OrderDetailVO;
import com.nameless.mall.order.api.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.*;

/**
 * 訂單服務 Controller，對外暴露訂單 RESTful API。
 * <p>
 * 負責範圍：
 * <ul>
 * <li>一般下單流程：防重送 Token 產生 → {@code submitOrder} 非同步建單 → 前端輪詢狀態</li>
 * <li>訂單查詢：分頁列表、依 orderSn 查詳情</li>
 * <li>訂單操作：取消、確認收貨</li>
 * <li>內部 Feign 端點（{@code /internal/*}）：僅供 payment-service 等服務呼叫，已由 Gateway
 * denyAll 封鎖外部存取</li>
 * </ul>
 * <p>
 * 非同步建單說明：{@code submitOrder} 返回的
 * {@link com.nameless.mall.order.api.vo.OrderVO}
 * 初始狀態為 {@code CREATING}，前端應透過 {@code GET /orders/{orderSn}/status} 輪詢，
 * 直到狀態轉為 {@code PENDING_PAYMENT} 後再導向付款頁面。
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 提交訂單的 API 端點
     * 
     * @param submitDTO 包含所有建立訂單所需資訊的 DTO
     * @return 建立成功的訂單資訊
     */
    @PostMapping
    @SentinelResource(value = "submitOrder", blockHandler = "submitOrderBlock")
    public Result<OrderVO> submitOrder(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrderSubmitDTO submitDTO) {
        OrderVO order = orderService.submitOrder(userId, submitDTO);
        return Result.ok(order, "訂單建立成功");
    }

    /** Sentinel 限流降級：提交訂單 */
    public Result<OrderVO> submitOrderBlock(Long userId, OrderSubmitDTO submitDTO, BlockException ex) {
        return Result.fail("系統繁忙，請稍後再試");
    }

    /** 產生防重送權杖（前端進入結帳頁時呼叫）。 */
    @GetMapping("/token")
    public Result<String> generateOrderToken(@RequestHeader("X-User-Id") Long userId) {
        String token = orderService.generateOrderToken(userId);
        return Result.ok(token);
    }

    /**
     * 分頁查詢當前登入使用者的訂單列表
     * 
     * @param pageNum  當前頁碼
     * @param pageSize 每頁顯示數量
     * @return 分頁後的訂單列表
     */
    @GetMapping
    public Result<Page<OrderVO>> getOrderList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status) {
        Page<OrderVO> orderPage = orderService.findPage(userId, pageNum, pageSize, status);
        return Result.ok(orderPage);
    }

    /**
     * 根據訂單的業務編號(orderSn)，查詢訂單的完整詳情
     * 
     * @param orderSn 訂單的業務編號
     * @return 包含訂單所有詳細資訊的 DTO
     */
    @GetMapping("/{orderSn}")
    public Result<OrderDetailVO> getOrderDetail(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderSn) {
        OrderDetailVO orderDetail = orderService.getOrderDetailBySn(userId, orderSn);
        return Result.ok(orderDetail);
    }

    /** 內部 Feign 介面：查詢訂單詳情（僅供微服務間呼叫）。 */
    @GetMapping("/internal/{orderSn}")
    public Result<OrderDetailDTO> getOrderDetailInternal(@PathVariable String orderSn) {
        // 直接從數據庫取，不走業務攔截，確保金流服務能取到金額
        OrderDetailDTO orderDetail = orderService.getOrderDetailInternal(orderSn);
        return Result.ok(orderDetail);
    }

    /**
     * 查詢訂單建立進度（前端輪詢用）。
     * 異步下單後，前端透過此端點等待 CREATING → PENDING_PAYMENT。
     */
    @GetMapping("/{orderSn}/status")
    public Result<OrderVO> getOrderStatus(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderSn) {
        OrderVO status = orderService.getOrderCreationStatus(userId, orderSn);
        return Result.ok(status);
    }

    /** 取消訂單。 */
    @PutMapping("/{orderSn}/cancel")
    public Result<Void> cancelOrder(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderSn) {
        orderService.cancelOrder(userId, orderSn);
        return Result.ok();
    }

    /**
     * 確認收貨
     * 
     * @param orderSn 訂單編號
     * @return 操作結果
     */
    @PutMapping("/{orderSn}/confirm-receipt")
    public Result<Void> confirmReceipt(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderSn) {
        orderService.confirmReceipt(userId, orderSn);
        return Result.ok();
    }
}
