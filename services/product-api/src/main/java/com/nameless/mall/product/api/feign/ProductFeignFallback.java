package com.nameless.mall.product.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.nameless.mall.core.domain.PageResult;
import com.nameless.mall.product.api.vo.ProductListVO;
import com.nameless.mall.product.api.vo.ProductDetailVO;

import java.util.List;

/**
 * ProductFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class ProductFeignFallback implements FallbackFactory<ProductFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignFallback.class);

    @Override
    public ProductFeignClient create(Throwable cause) {
        return new ProductFeignClient() {
            @Override
            public Result<VariantDTO> getVariantById(Long variantId) {
                log.error("降級 | ProductFeignClient.getVariantById 失敗, variantId: {}, cause: {}",
                        variantId, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，請稍後重試");
            }

            @Override
            public Result<Void> decreaseStock(List<DecreaseStockInputDTO> dtoList) {
                log.error("降級 | ProductFeignClient.decreaseStock 失敗, 數量: {}, cause: {}",
                        dtoList != null ? dtoList.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，無法完成訂單");
            }

            @Override
            public Result<Void> increaseStock(List<DecreaseStockInputDTO> dtoList) {
                log.error("降級 | ProductFeignClient.increaseStock 失敗, 數量: {}, cause: {}",
                        dtoList != null ? dtoList.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，庫存返還將稍後重試");
            }

            @Override
            public Result<List<VariantDTO>> getVariantsBatch(List<Long> ids) {
                log.error("降級 | ProductFeignClient.getVariantsBatch 失敗, 數量: {}, cause: {}",
                        ids != null ? ids.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用");
            }

            @Override
            public Result<List<VariantDTO>> getVariantsByProductIds(List<Long> productIds) {
                log.error("降級 | ProductFeignClient.getVariantsByProductIds 失敗, 數量: {}, cause: {}",
                        productIds != null ? productIds.size() : 0, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用");
            }

            @Override
            public Result<List<CategoryDTO>> getCategoryTree() {
                log.error("降級 | ProductFeignClient.getCategoryTree 失敗, cause: {}",
                        cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用");
            }

            @Override
            public Result<PageResult<ProductListVO>> getProductList(Integer pageNum, Integer pageSize) {
                log.error("降級 | ProductFeignClient.getProductList 失敗, page: {}/{}, cause: {}",
                        pageNum, pageSize, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，同步將稍後重試");
            }

            @Override
            public Result<ProductDetailVO> getProductDetail(Long id) {
                log.error("降級 | ProductFeignClient.getProductDetail 失敗, productId: {}, cause: {}",
                        id, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，請稍後重試");
            }
        };
    }
}
