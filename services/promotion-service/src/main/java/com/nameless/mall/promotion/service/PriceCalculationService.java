package com.nameless.mall.promotion.service;

import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;

import java.util.List;

/**
 * 促銷計價服務介面。
 * 整合秒殺與行銷活動，計算商品最優價格。
 */
public interface PriceCalculationService {

    /**
     * 計算商品列表的最佳價格。
     */
    List<ProductPriceResultDTO> calculateBestPrices(List<ProductPriceCheckDTO> checkList);
}
