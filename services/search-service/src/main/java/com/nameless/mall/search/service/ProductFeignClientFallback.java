package com.nameless.mall.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.product.api.dto.ProductDTO;
import com.nameless.mall.product.api.vo.ProductDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * ProductFeignClient (search-service) 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class ProductFeignClientFallback implements FallbackFactory<ProductFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignClientFallback.class);

    @Override
    public ProductFeignClient create(Throwable cause) {
        return new ProductFeignClient() {
            @Override
            public Result<Page<ProductDTO>> getProductList(Integer pageNum, Integer pageSize) {
                log.error("降級 | ProductFeignClient.getProductList 失敗, page: {}/{}, cause: {}",
                        pageNum, pageSize, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "商品服務暫時不可用，搜尋同步將稍後重試");
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
