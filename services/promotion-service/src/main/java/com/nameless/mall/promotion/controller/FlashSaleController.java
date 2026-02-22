package com.nameless.mall.promotion.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.FlashSalePromotionDTO;
import com.nameless.mall.promotion.api.dto.FlashSaleSkuDTO;
import com.nameless.mall.promotion.api.vo.FlashSalePromotionVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSkuVO;
import com.nameless.mall.promotion.exception.SentinelBlockHandler;
import com.nameless.mall.promotion.service.FlashSalePromotionService;
import com.nameless.mall.promotion.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 限時特賣 Controller。
 * 提供特賣活動查詢與庫存扣減 API。
 */
@RestController
@RequestMapping("/promotions/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final FlashSalePromotionService flashSalePromotionService;

    /**
     * 獲取當前進行中的限時特賣活動列表。
     */
    @GetMapping
    public Result<List<FlashSalePromotionVO>> getCurrentPromotions() {
        List<FlashSalePromotionDTO> dtos = flashSaleService.getCurrentPromotions();
        List<FlashSalePromotionVO> vos = dtos.stream().map(dto -> {
            FlashSalePromotionVO vo = new FlashSalePromotionVO();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(vos);
    }

    /**
     * 獲取指定活動詳情。
     * 活動不存在時拋出 {@code PROMOTION_NOT_FOUND}，回傳 HTTP 404。
     */
    @GetMapping("/{id}")
    public Result<FlashSalePromotionVO> getPromotionById(@PathVariable Long id) {
        FlashSalePromotionDTO dto = flashSaleService.getPromotionById(id);
        // 活動不存在應明確拋出 404，而非回傳 ok(null) 讓呼叫方自行猜測
        if (dto == null) {
            throw new BusinessException(ResultCodeEnum.PROMOTION_NOT_FOUND);
        }
        FlashSalePromotionVO vo = new FlashSalePromotionVO();
        BeanUtils.copyProperties(dto, vo);
        return Result.ok(vo);
    }

    /**
     * 獲取指定活動的商品列表。
     */
    @GetMapping("/{promotionId}/skus")
    public Result<List<FlashSaleSkuVO>> getSkusByPromotionId(@PathVariable Long promotionId) {
        List<FlashSaleSkuDTO> dtos = flashSaleService.getSkusByPromotionId(promotionId);
        List<FlashSaleSkuVO> vos = dtos.stream().map(dto -> {
            FlashSaleSkuVO vo = new FlashSaleSkuVO();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(vos);
    }

    /**
     * 獲取當前秒殺場次（首頁展示用）。
     */
    @GetMapping("/current")
    @SentinelResource(value = "getCurrentSession", blockHandler = "handleGetCurrentSessionBlock", blockHandlerClass = SentinelBlockHandler.class)
    public Result<FlashSaleSessionVO> getCurrentSession() {
        return Result.ok(flashSalePromotionService.getCurrentSession());
    }

    /**
     * 扣減特賣庫存（供 Order Service 內部呼叫）。
     * <p>
     * 此端點直接操作 Redis 庫存，高併發時若無保護會造成
     * 大量請求同時衝擊服務。加上 {@code @SentinelResource} 限流，
     * 超過閾值後由 {@link SentinelBlockHandler#handleBlock} 控制降級。
     */
    @PostMapping("/deduct-stock")
    @SentinelResource(value = "deductStock", blockHandler = "handleDeductStockBlock", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Void> deductStock(@RequestBody List<FlashSaleDeductionDTO> deductionList) {
        flashSaleService.deductStock(deductionList);
        return Result.ok();
    }

    /**
     * 返還特賣庫存（供 Order Service 內部呼叫）。
     * <p>
     * 庫存返還同樣需要 Sentinel 保護，避免補償風暴衝擊服務。
     */
    @PostMapping("/recover-stock/{orderSn}")
    @SentinelResource(value = "recoverStock", blockHandler = "handleRecoverStockBlock", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Void> recoverStock(@PathVariable String orderSn) {
        flashSaleService.recoverStock(orderSn);
        return Result.ok();
    }

    /**
     * [Admin] 強制同步庫存 (Redis 預熱)
     * 用於解決系統啟動後 Redis 庫存缺失問題。
     * <p>
     * 加上 Sentinel 防止重複觸發或惡意呼叫壓垮服務。
     */
    @PostMapping("/sync-stock")
    @SentinelResource(value = "syncStock", blockHandler = "handleSyncStockBlock", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Void> syncStock() {
        flashSaleService.syncStock();
        return Result.ok();
    }
}
