package com.nameless.mall.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.product.api.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 遠程呼叫 商品服務
 */
@FeignClient(name = "product-service", fallbackFactory = ProductFeignClientFallback.class)
public interface ProductFeignClient {

        /**
         * 從商品服務取得所有商品清單 (用於同步)
         */
        @GetMapping("/products")
        com.nameless.mall.core.domain.Result<Page<ProductDTO>> getProductList(
                        @RequestParam("pageNum") Integer pageNum,
                        @RequestParam("pageSize") Integer pageSize);

        /**
         * 取得單筆商品詳情 (用於同步單個 ES Document)
         */
        @GetMapping("/products/{id}")
        com.nameless.mall.core.domain.Result<com.nameless.mall.product.api.vo.ProductDetailVO> getProductDetail(
                        @org.springframework.web.bind.annotation.PathVariable("id") Long id);
}
