package com.nameless.mall.promotion.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.FlashSalePromotionDTO;
import com.nameless.mall.promotion.api.dto.FlashSaleSkuDTO;
import com.nameless.mall.promotion.api.vo.FlashSalePromotionVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSkuVO;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;
import com.nameless.mall.promotion.service.FlashSalePromotionService;
import com.nameless.mall.promotion.service.FlashSaleService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.nameless.mall.promotion.exception.SentinelBlockHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     */
    @GetMapping("/{id}")
    public Result<FlashSalePromotionVO> getPromotionById(@PathVariable Long id) {
        FlashSalePromotionDTO dto = flashSaleService.getPromotionById(id);
        if (dto == null) {
            return Result.ok(null);
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
    @SentinelResource(value = "getCurrentSession", blockHandler = "handleBlock", blockHandlerClass = SentinelBlockHandler.class)
    public Result<FlashSaleSessionVO> getCurrentSession() {
        return Result.ok(flashSalePromotionService.getCurrentSession());
    }

    /**
     * 扣減特賣庫存（供 Order Service 內部呼叫）。
     */
    @PostMapping("/deduct-stock")
    public Result<Void> deductStock(@RequestBody List<FlashSaleDeductionDTO> deductionList) {
        flashSaleService.deductStock(deductionList);
        return Result.ok();
    }

    /**
     * 返還特賣庫存（供 Order Service 內部呼叫）。
     */
    @PostMapping("/recover-stock/{orderSn}")
    public Result<Void> recoverStock(@PathVariable String orderSn) {
        flashSaleService.recoverStock(orderSn);
        return Result.ok();
    }

    /**
     * [Admin] 強制同步庫存 (Redis 預熱)
     * 用於解決系統啟動後 Redis 庫存缺失問題。
     */
    @PostMapping("/sync-stock")
    public Result<Void> syncStock() {
        // 管理員強制同步 Redis 庫存
        flashSaleService.syncStock();
        return Result.ok();
    }
}
