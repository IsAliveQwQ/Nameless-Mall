package com.nameless.mall.promotion.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSkuVO;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 促銷服務 Feign Client
 */
@FeignClient(name = "promotion-service", path = "/promotions", fallbackFactory = PromotionFeignFallback.class)
public interface PromotionFeignClient {

        /**
         * 獲取當前秒殺場次 (包含商品列表)
         * 用於 Order Service 計算特賣價格
         */
        @GetMapping("/flash-sales/current")
        Result<FlashSaleSessionVO> getCurrentSession();

        /**
         * 取得特定活動下的所有商品 SKU
         */
        @GetMapping("/flash-sales/{promotionId}/skus")
        Result<List<FlashSaleSkuVO>> getSkusByPromotionId(
                        @PathVariable("promotionId") Long promotionId);

        /**
         * 扣減特賣庫存 (支援冪等性與限購檢查)
         * 會檢查 flash_sale_logs 避免重複扣減
         */
        @PostMapping("/flash-sales/deduct-stock")
        Result<Void> deductStock(
                        @RequestBody List<FlashSaleDeductionDTO> deductionList);

        /**
         * 返還特賣庫存 (基於訂單編號)
         */
        @PostMapping("/flash-sales/recover-stock/{orderSn}")
        Result<Void> recoverStock(@PathVariable("orderSn") String orderSn);

        /**
         * 計算商品最佳價格 (整合特賣與常駐活動)
         * 購物車與訂單確認頁面專用
         */
        @PostMapping("/price/calculate")
        Result<List<ProductPriceResultDTO>> calculateBestPrices(
                        @RequestBody List<ProductPriceCheckDTO> checkList);
}
