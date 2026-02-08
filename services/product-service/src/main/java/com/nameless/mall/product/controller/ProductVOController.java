package com.nameless.mall.product.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.product.api.vo.ProductDetailVO;
import com.nameless.mall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品展示控制器 (V2)
 * 專門負責 VO 的回傳，不觸擊舊有的 ProductController
 */
@Tag(name = "商品展示 (V2)", description = "提供詳情頁 VO 等新版 API")
@RestController
@RequestMapping("/products/v2")
@RequiredArgsConstructor
public class ProductVOController {

    private final ProductService productService;

    @Operation(summary = "查詢商品詳細資訊 (VO版)")
    @GetMapping("/detail/{id}")
    public Result<ProductDetailVO> getProductDetailVO(@PathVariable("id") Long id) {
        ProductDetailVO detail = productService.getProductDetailVOById(id);
        return Result.ok(detail);
    }
}
