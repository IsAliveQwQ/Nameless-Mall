package com.nameless.mall.promotion.client;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Product Service Feign Client。
 * 負責與商品服務通訊，獲取商品與規格資訊。
 */
@FeignClient(name = "product-service", contextId = "productClient", fallbackFactory = ProductClientFallback.class)
public interface ProductClient {

    /**
     * 批次查詢規格資訊（包含商品名稱與主圖）。
     */
    @PostMapping("/products/internal/variants/batch")
    Result<List<VariantDTO>> getVariantsBatch(@RequestBody List<Long> ids);

    /**
     * 根據商品 ID 列表查詢所有規格。
     */
    @PostMapping("/products/internal/variants/by-products")
    Result<List<VariantDTO>> getVariantsByProductIds(@RequestBody List<Long> productIds);

    /**
     * 查詢所有商品分類樹。
     */
    @GetMapping("/products/categories/tree")
    Result<List<CategoryDTO>> getCategoryTree();
}
