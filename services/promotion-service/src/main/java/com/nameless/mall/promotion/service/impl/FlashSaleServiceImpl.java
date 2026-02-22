package com.nameless.mall.promotion.service.impl;

import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.FlashSalePromotionDTO;
import com.nameless.mall.promotion.api.dto.FlashSaleSkuDTO;
import com.nameless.mall.promotion.service.FlashSalePromotionService;
import com.nameless.mall.promotion.service.FlashSaleService;
import com.nameless.mall.promotion.service.FlashSaleSkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 限時特賣服務 Facade 實作
 * <p>
 * 整合 FlashSalePromotionService 與 FlashSaleSkuService，提供給 Controller 統一入口。
 */
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements FlashSaleService {

    private final FlashSalePromotionService flashSalePromotionService;
    private final FlashSaleSkuService flashSaleSkuService;

    @Override
    public List<FlashSalePromotionDTO> getCurrentPromotions() {
        return flashSalePromotionService.getCurrentPromotions();
    }

    @Override
    public FlashSalePromotionDTO getPromotionById(Long id) {
        return flashSalePromotionService.getPromotionById(id);
    }

    @Override
    public List<FlashSaleSkuDTO> getSkusByPromotionId(Long promotionId) {
        return flashSaleSkuService.getByPromotionId(promotionId);
    }

    @Override
    public void deductStock(List<FlashSaleDeductionDTO> deductionList) {
        // 委派核心扣減邏輯給 FlashSalePromotionService (確保一致的鎖與事務)
        flashSalePromotionService.deductStock(deductionList);
    }

    @Override
    public void recoverStock(String orderSn) {
        // 委派核心返還邏輯給 FlashSalePromotionService
        flashSalePromotionService.recoverStock(orderSn);
    }

    @Override
    public void syncStock() {
        flashSalePromotionService.syncPromotionStock();
    }
}
