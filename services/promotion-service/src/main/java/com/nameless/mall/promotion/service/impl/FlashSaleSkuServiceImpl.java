package com.nameless.mall.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.promotion.api.dto.FlashSaleSkuDTO;
import com.nameless.mall.promotion.entity.FlashSaleSku;
import com.nameless.mall.promotion.mapper.FlashSaleSkuMapper;
import com.nameless.mall.promotion.service.FlashSaleSkuService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 特賣商品服務實現。
 * 負責管理特賣商品的資料查詢與基礎庫存扣減。
 */
@Service
public class FlashSaleSkuServiceImpl extends ServiceImpl<FlashSaleSkuMapper, FlashSaleSku>
        implements FlashSaleSkuService {

    /**
     * 根據活動 ID 獲取所有參與的 SKU 列表。
     */
    @Override
    public List<FlashSaleSkuDTO> getByPromotionId(Long promotionId) {
        return this.list(new LambdaQueryWrapper<FlashSaleSku>().eq(FlashSaleSku::getPromotionId, promotionId))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 獲取單一特賣 SKU 詳情。
     */
    @Override
    public FlashSaleSkuDTO getSkuById(Long id) {
        FlashSaleSku sku = this.getById(id);
        return sku != null ? toDTO(sku) : null;
    }

    /**
     * 原子扣減資料庫庫存。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void decreaseStock(Long skuId, Integer quantity) {
        // 1. 嘗試原子扣減庫存（WHERE stock >= quantity）
        if (baseMapper.decreaseStock(skuId, quantity) == 0) {
            // 2. 扣減失敗：區分「商品不存在」與「庫存不足」
            if (this.getById(skuId) == null) {
                throw new BusinessException(ResultCodeEnum.PROMOTION_SKU_NOT_FOUND);
            }
            throw new BusinessException(ResultCodeEnum.PROMOTION_STOCK_INSUFFICIENT);
        }
    }

    /** 將實體類轉換為 DTO（BeanUtils 屬性拷貝）。 */
    private FlashSaleSkuDTO toDTO(FlashSaleSku entity) {
        FlashSaleSkuDTO dto = new FlashSaleSkuDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
