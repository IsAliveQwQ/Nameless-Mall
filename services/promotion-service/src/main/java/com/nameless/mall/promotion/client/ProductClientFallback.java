package com.nameless.mall.promotion.client;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ProductClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class ProductClientFallback implements FallbackFactory<ProductClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFallback.class);

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public Result<List<VariantDTO>> getVariantsBatch(List<Long> ids) {
                log.error("降級 | ProductClient.getVariantsBatch 失敗, 數量: {}, cause: {}",
                        ids != null ? ids.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，請稍後重試");
            }

            @Override
            public Result<List<VariantDTO>> getVariantsByProductIds(List<Long> productIds) {
                log.error("降級 | ProductClient.getVariantsByProductIds 失敗, 數量: {}, cause: {}",
                        productIds != null ? productIds.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，請稍後重試");
            }

            @Override
            public Result<List<CategoryDTO>> getCategoryTree() {
                log.error("降級 | ProductClient.getCategoryTree 失敗, cause: {}",
                        cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，請稍後重試");
            }
        };
    }
}
