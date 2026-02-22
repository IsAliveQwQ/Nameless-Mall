package com.nameless.mall.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.FlashSalePromotionDTO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.entity.FlashSalePromotion;

import java.util.List;

/**
 * 限時特賣活動服務介面。
 * 負責管理限時特賣活動的業務邏輯。
 */
public interface FlashSalePromotionService extends IService<FlashSalePromotion> {

    /**
     * 獲取當前進行中的活動列表。
     */
    List<FlashSalePromotionDTO> getCurrentPromotions();

    /**
     * 根據活動 ID 獲取活動詳情。
     */
    FlashSalePromotionDTO getPromotionById(Long id);

    /**
     * 獲取當前秒殺場次（首頁展示用）。
     */
    FlashSaleSessionVO getCurrentSession();

    /**
     * 扣減特賣庫存（事務性操作）。
     */
    void deductStock(List<FlashSaleDeductionDTO> deductionList);

    /**
     * 返還特賣庫存。
     */
    void recoverStock(String orderSn);

    /**
     * 同步特賣庫存（預熱用）。
     */
    void syncPromotionStock();
}
