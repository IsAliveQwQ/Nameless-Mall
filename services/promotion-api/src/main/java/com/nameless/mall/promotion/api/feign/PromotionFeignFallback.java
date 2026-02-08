package com.nameless.mall.promotion.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSkuVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PromotionFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class PromotionFeignFallback implements FallbackFactory<PromotionFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(PromotionFeignFallback.class);

    @Override
    public PromotionFeignClient create(Throwable cause) {
        return new PromotionFeignClient() {
            @Override
            public Result<FlashSaleSessionVO> getCurrentSession() {
                log.error("降級 | PromotionFeignClient.getCurrentSession 失敗, cause: {}",
                        cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "促銷服務暫時不可用");
            }

            @Override
            public Result<List<FlashSaleSkuVO>> getSkusByPromotionId(Long promotionId) {
                log.error("降級 | PromotionFeignClient.getSkusByPromotionId 失敗, promotionId: {}, cause: {}",
                        promotionId, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "促銷服務暫時不可用");
            }

            @Override
            public Result<Void> deductStock(List<FlashSaleDeductionDTO> deductionList) {
                log.error("降級 | PromotionFeignClient.deductStock 失敗, 數量: {}, cause: {}",
                        deductionList != null ? deductionList.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "促銷服務異常，無法完成特賣扣減");
            }

            @Override
            public Result<Void> recoverStock(String orderSn) {
                log.error("降級 | PromotionFeignClient.recoverStock 失敗, orderSn: {}, cause: {}",
                        orderSn, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "促銷服務異常，無法完成特賣返還");
            }

            @Override
            public Result<List<ProductPriceResultDTO>> calculateBestPrices(List<ProductPriceCheckDTO> checkList) {
                log.error("降級 | PromotionFeignClient.calculateBestPrices 失敗, 數量: {}, cause: {}",
                        checkList != null ? checkList.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "計價引擎目前離線");
            }
        };
    }
}
