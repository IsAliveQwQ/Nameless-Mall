package com.nameless.mall.promotion.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import com.nameless.mall.promotion.service.PriceCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 統一計價 Controller
 * 提供商品價格計算 API，整合特賣與常駐活動
 */
@RestController
@RequestMapping("/promotions/price")
@RequiredArgsConstructor
public class PricingController {

    private final PriceCalculationService priceCalculationService;

    /**
     * 計算商品最佳價格
     */
    @PostMapping("/calculate")
    public Result<List<ProductPriceResultDTO>> calculateBestPrices(@RequestBody List<ProductPriceCheckDTO> checkList) {
        List<ProductPriceResultDTO> dtos = priceCalculationService.calculateBestPrices(checkList);
        return Result.ok(dtos);
    }
}
