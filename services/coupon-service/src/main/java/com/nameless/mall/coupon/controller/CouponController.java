package com.nameless.mall.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.coupon.api.dto.CouponCalculationDTO;
import com.nameless.mall.coupon.api.dto.CouponCalculationResult;
import com.nameless.mall.coupon.api.dto.CouponUseInputDTO;
import com.nameless.mall.coupon.api.vo.ApplicableCouponVO;
import com.nameless.mall.coupon.api.vo.CouponCardVO;
import com.nameless.mall.coupon.api.vo.CouponTemplateVO;
import com.nameless.mall.coupon.api.vo.UserCouponVO;

import com.nameless.mall.coupon.service.CouponQueryService;
import com.nameless.mall.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 優惠券控制器
 */
@Tag(name = "優惠券管理", description = "優惠券相關 API")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponQueryService couponQueryService;

    @Operation(summary = "獲取可領取的優惠券列表")
    @GetMapping("/available")
    public Result<List<CouponTemplateVO>> getAvailableTemplates() {
        return Result.ok(couponQueryService.getAvailableTemplatesVO());
    }

    @Operation(summary = "領取優惠券")
    @PostMapping("/{templateId}/claim")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Void> claimCoupon(
            @PathVariable Long templateId,
            @RequestHeader("X-User-Id") Long userId) {
        couponService.claimCoupon(templateId, userId);
        return Result.ok();
    }

    @Operation(summary = "獲取我的優惠券列表")
    @GetMapping("/my")
    public Result<Page<UserCouponVO>> getMyCoupons(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(couponQueryService.getMyCouponsVO(userId, pageNum, pageSize));
    }

    @Operation(summary = "獲取可用於訂單的優惠券")
    @GetMapping("/usable")
    public Result<List<UserCouponVO>> getUsableCoupons(
            @RequestParam BigDecimal amount,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(couponQueryService.getUsableCouponsVO(userId, amount));
    }

    @Operation(summary = "核銷優惠券 (內部呼叫)", hidden = true)
    @PostMapping("/internal/use")
    public Result<Void> useCoupon(@Valid @RequestBody CouponUseInputDTO dto) {
        couponService.useCoupon(dto.getUserCouponId(), dto.getOrderSn());
        return Result.ok();
    }

    @Operation(summary = "退還優惠券 (內部呼叫)", hidden = true)
    @PostMapping("/internal/return")
    public Result<Void> returnCoupon(@RequestParam("userCouponId") Long userCouponId) {
        couponService.returnCoupon(userCouponId);
        return Result.ok();
    }

    // ============ 內部服務呼叫 (Feign) ============

    @Operation(summary = "試算優惠券折扣 (內部呼叫)", hidden = true)
    @PostMapping("/internal/calculate")
    public Result<CouponCalculationResult> calculateDiscountInternal(
            @Valid @RequestBody CouponCalculationDTO dto) {
        return Result.ok(couponService.calculateDiscount(dto));
    }

    // ============ 前端公開接口 (BFF) ============

    @Operation(summary = "獲取訂單可用的優惠券列表 (結帳頁用)")
    @PostMapping("/applicable")
    public Result<List<ApplicableCouponVO>> getApplicableCoupons(
            @Valid @RequestBody CouponCalculationDTO dto,
            @RequestHeader("X-User-Id") Long userId) {
        // 安全填充 userId
        dto.setUserId(userId);
        return Result.ok(couponService.getApplicableCoupons(dto));
    }

    @Operation(summary = "Internal: 獲取訂單可用的優惠券列表", hidden = true)
    @PostMapping("/internal/applicable")
    public Result<List<ApplicableCouponVO>> getApplicableCouponsInternal(
            @Valid @RequestBody CouponCalculationDTO dto) {
        return Result.ok(couponService.getApplicableCoupons(dto));
    }

    @Operation(summary = "試算優惠券折扣 (預覽用)")
    @PostMapping("/calculate")
    public Result<CouponCalculationResult> calculateDiscount(
            @Valid @RequestBody CouponCalculationDTO dto,
            @RequestHeader("X-User-Id") Long userId) {
        // 安全填充 userId
        dto.setUserId(userId);
        return Result.ok(couponService.calculateDiscount(dto));
    }

    @Operation(summary = "獲取優惠券卡片列表 (前端展示)")
    @GetMapping("/cards")
    public Result<List<CouponCardVO>> getCouponCards(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(couponQueryService.getCouponCards(userId));
    }
}
