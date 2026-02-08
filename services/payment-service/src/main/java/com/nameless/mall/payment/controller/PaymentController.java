package com.nameless.mall.payment.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.payment.api.dto.PaymentConfirmDTO;
import com.nameless.mall.payment.api.dto.PaymentCreateDTO;
import com.nameless.mall.payment.api.dto.PaymentDTO;
import com.nameless.mall.payment.api.vo.PaymentVO;
import com.nameless.mall.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器
 */
@Tag(name = "支付管理", description = "支付相關 API")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 創建支付單（用戶端）
     */
    @Operation(summary = "創建支付單")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<PaymentVO> createPayment(
            @Valid @RequestBody PaymentCreateDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PaymentDTO payment = paymentService.createPayment(dto, userId);
        return Result.ok(toVO(payment));
    }

    /**
     * 查詢支付單狀態（用戶端）
     */
    @Operation(summary = "查詢支付單")
    @GetMapping("/{paymentSn}")
    public Result<PaymentVO> getPayment(@PathVariable String paymentSn) {
        PaymentDTO payment = paymentService.getPaymentByPaymentSn(paymentSn);
        return Result.ok(toVO(payment));
    }

    /**
     * 確認付款（銀行轉帳 - 用戶端）
     */
    @Operation(summary = "確認付款（銀行轉帳）")
    @PostMapping("/confirm")
    public Result<Void> confirmPayment(
            @Valid @RequestBody PaymentConfirmDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        paymentService.confirmPayment(dto.getPaymentSn(), dto.getAccountInfo(), userId);
        return Result.ok(null, "付款確認成功");
    }

    /**
     * 處理第三方金流回調
     * <p>
     * 前端頁面跳轉回調或第三方 Server Webhook 用
     * </p>
     */
    @Operation(summary = "處理第三方金流回調")
    @PostMapping("/callback/{provider}")
    public Result<Object> processCallback(
            @PathVariable String provider,
            @RequestBody Map<String, String> params) {
        // 委派給 Service 處理
        Object result = paymentService.handleCallback(provider, params);
        return Result.ok(result);
    }

    /**
     * ECPay 專用回調端點
     * <p>
     * 綠界回調格式為 application/x-www-form-urlencoded，
     * 成功需回傳純文字 "1|OK"，否則綠界會重試。
     * </p>
     */
    @Operation(summary = "綠界金流回調 (ECPay)")
    @PostMapping(value = "/callback/ecpay", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> processECPayCallback(
            @RequestParam Map<String, String> params) {
        try {
            paymentService.handleCallback("ECPAY", params);
            // 綠界規範：成功回傳 "1|OK"
            return ResponseEntity.ok("1|OK");
        } catch (Exception e) {
            // 失敗回傳 "0|Error"，綠界會重試
            return ResponseEntity.ok("0|" + e.getMessage());
        }
    }

    /**
     * 創建支付單（內部呼叫）
     */
    @PostMapping("/internal/create")
    public Result<PaymentDTO> createPaymentInternal(@RequestBody PaymentCreateDTO dto) {
        // 內部呼叫不需要 userId 驗證，由呼叫方保證
        PaymentDTO payment = paymentService.createPayment(dto, null);
        return Result.ok(payment);
    }

    /**
     * 查詢支付狀態（內部呼叫）
     */
    @GetMapping("/internal/status/{orderSn}")
    public Result<PaymentDTO> getPaymentByOrderSn(@PathVariable String orderSn) {
        PaymentDTO payment = paymentService.getPaymentByOrderSn(orderSn);
        return Result.ok(payment);
    }

    /**
     * 取消支付單（內部呼叫）
     */
    @PostMapping("/internal/cancel/{orderSn}")
    public Result<Void> cancelPayment(@PathVariable String orderSn) {
        paymentService.cancelPayment(orderSn);
        return Result.ok();
    }

    /**
     * DTO 轉 VO 轉換器
     */
    private PaymentVO toVO(PaymentDTO dto) {
        if (dto == null) {
            return null;
        }
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }
}
