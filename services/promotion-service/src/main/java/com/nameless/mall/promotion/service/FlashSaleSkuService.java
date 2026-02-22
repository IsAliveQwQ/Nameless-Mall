package com.nameless.mall.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.promotion.api.dto.FlashSaleSkuDTO;
import com.nameless.mall.promotion.entity.FlashSaleSku;

import java.util.List;

/**
 * 限時特賣商品服務介面
 * <p>
 * 負責管理限時特賣商品的業務邏輯。
 */
public interface FlashSaleSkuService extends IService<FlashSaleSku> {

    /**
     * 根據活動 ID 獲取商品列表
     * @param promotionId 活動 ID
     * @return 特賣商品 DTO 列表
     */
    List<FlashSaleSkuDTO> getByPromotionId(Long promotionId);

    /**
     * 根據 ID 獲取特賣商品詳情
     * @param id 特賣商品 ID
     * @return 特賣商品 DTO
     */
    FlashSaleSkuDTO getSkuById(Long id);

    /**
     * 扣減特賣庫存（原子操作）
     * @param skuId 特賣商品 ID
     * @param quantity 扣減數量
     * @throws com.nameless.mall.core.exception.BusinessException 若庫存不足
     */
    void decreaseStock(Long skuId, Integer quantity);
}
