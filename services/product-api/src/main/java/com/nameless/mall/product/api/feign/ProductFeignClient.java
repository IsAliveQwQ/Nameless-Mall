package com.nameless.mall.product.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.nameless.mall.core.domain.PageResult;
import com.nameless.mall.product.api.vo.ProductListVO;
import com.nameless.mall.product.api.vo.ProductDetailVO;

import java.util.List;

/**
 * 商品服務的 Feign 客戶端接口
 */
@FeignClient(name = "product-service", fallbackFactory = ProductFeignFallback.class)
public interface ProductFeignClient {

    /**
     * 根據規格 ID 查詢擴充後的 VariantDTO。
     */
    @GetMapping("/products/internal/variant/{variantId}")
    Result<VariantDTO> getVariantById(@PathVariable("variantId") Long variantId);

    /**
     * 批次扣減商品庫存。
     */
    @PostMapping("/products/internal/decrease-stock")
    Result<Void> decreaseStock(@RequestBody List<DecreaseStockInputDTO> dtoList);

    /**
     * 批次返還商品庫存。
     */
    @PostMapping("/products/internal/increase-stock")
    Result<Void> increaseStock(@RequestBody List<DecreaseStockInputDTO> dtoList);

    /**
     * 批次查詢規格資訊（包含商品名稱與主圖）。
     * 提供給 Promotion Service 等批量獲取商品資訊使用。
     */
    @PostMapping("/products/internal/variants/batch")
    Result<List<VariantDTO>> getVariantsBatch(@RequestBody List<Long> ids);

    /**
     * 根據商品 ID 列表查詢所有規格。
     * 提供給 Promotion Service 獲取完整商品規格使用。
     */
    @PostMapping("/products/internal/variants/by-products")
    Result<List<VariantDTO>> getVariantsByProductIds(@RequestBody List<Long> productIds);

    /**
     * 查詢所有商品分類樹。
     */
    @GetMapping("/products/categories/internal/tree")
    Result<List<CategoryDTO>> getCategoryTree();

    /**
     * 從商品服務取得所有商品清單 (用於同步)
     */
    @GetMapping("/products")
    Result<PageResult<ProductListVO>> getProductList(
            @RequestParam("pageNum") Integer pageNum,
            @RequestParam("pageSize") Integer pageSize);

    /**
     * 取得單個商品詳情 (包含規格)，供 Search 等服務全量同步使用。
     */
    @GetMapping("/products/{id}")
    Result<ProductDetailVO> getProductDetail(
            @PathVariable("id") Long id);

}