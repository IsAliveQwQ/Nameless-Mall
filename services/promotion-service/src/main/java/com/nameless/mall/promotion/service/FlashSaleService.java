package com.nameless.mall.promotion.service;

import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.FlashSalePromotionDTO;
import com.nameless.mall.promotion.api.dto.FlashSaleSkuDTO;

import java.util.List;

/**
 * 限時特賣服務 Facade 介面。
 * 提供統一的限時特賣相關操作入口。
 */
public interface FlashSaleService {

    /**
     * 獲取當前進行中的活動列表。
     */
    List<FlashSalePromotionDTO> getCurrentPromotions();

    /**
     * 根據活動 ID 獲取活動詳情。
     */
    FlashSalePromotionDTO getPromotionById(Long id);

    /**
     * 獲取指定活動的商品列表。
     */
    List<FlashSaleSkuDTO> getSkusByPromotionId(Long promotionId);

    /**
     * 扣減特賣庫存。
     */
    void deductStock(List<FlashSaleDeductionDTO> deductionList);

    /**
     * 返還特賣庫存。
     */
    void recoverStock(String orderSn);

    /**
     * [Admin] 同步特賣庫存 (Redis 預熱)。
     */
    void syncStock();
}
