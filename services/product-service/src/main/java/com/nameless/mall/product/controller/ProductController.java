package com.nameless.mall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.core.domain.PageResult;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.dto.ProductDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.product.api.vo.ProductDetailVO;
import com.nameless.mall.product.api.vo.ProductListVO;
import com.nameless.mall.product.service.ProductService;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;
import com.nameless.mall.product.service.VariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理 Controller
 */
@Tag(name = "商品管理", description = "提供商品查詢、列表等 API")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final VariantService variantService;

    /**
     * 分頁查詢商品列表
     */
    @Operation(summary = "分頁查詢商品列表")
    @GetMapping({ "", "/" })
    public Result<PageResult<ProductListVO>> getProductList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        log.info("【商品查詢】分頁查詢: pageNum={}, pageSize={}, categoryId={}", pageNum, pageSize, categoryId);
        Page<ProductDTO> productPage = productService.getProductList(pageNum, pageSize, categoryId);

        List<ProductListVO> voList = productPage.getRecords().stream().map(dto -> {
            ProductListVO vo = new ProductListVO();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        }).collect(Collectors.toList());

        PageResult<ProductListVO> voPage = new PageResult<>(voList, productPage.getTotal(), productPage.getSize(),
                productPage.getCurrent());

        return Result.ok(voPage);
    }

    @Operation(summary = "查詢商品詳細資訊")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getProductDetail(
            @Parameter(description = "商品的唯一ID") @PathVariable("id") Long id) {
        ProductDetailVO productDetail = productService.getProductDetailVOById(id);
        if (productDetail == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND, "找不到商品 ID: " + id);
        }
        return Result.ok(productDetail);
    }

    /** 查詢商品規格資訊（內部 Feign 專用）。 */
    @Operation(summary = "查詢商品規格資訊", hidden = true)
    @GetMapping("/internal/variant/{variantId}")
    public Result<VariantDTO> getVariantForCart(@PathVariable("variantId") Long variantId) {
        log.info("【內部呼叫】查詢商品規格 ID: {}", variantId);
        VariantDTO variant = variantService.getVariantForCart(variantId);
        return Result.ok(variant);
    }

    /** 批次扣減庫存（內部 Feign 專用）。 */
    @Operation(summary = "批次扣減庫存 (內部 Feign 專用)", hidden = true)
    @PostMapping("/internal/decrease-stock")
    public Result<Void> decreaseStock(@RequestBody List<DecreaseStockInputDTO> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            log.warn("【庫存操作】扣減清單為空，跳過操作");
            return Result.ok();
        }
        variantService.decreaseStock(dtoList);
        return Result.ok();
    }

    /** 批次返還庫存（內部 Feign 專用）。 */
    @Operation(summary = "批次返還庫存 (內部 Feign 專用)", hidden = true)
    @PostMapping("/internal/increase-stock")
    public Result<Void> increaseStock(@RequestBody List<DecreaseStockInputDTO> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            log.warn("【庫存操作】返還清單為空，跳過操作");
            return Result.ok();
        }
        variantService.increaseStock(dtoList);
        return Result.ok();
    }

    /** 批次查詢規格資訊（內部 Feign 專用）。 */
    @Operation(summary = "批次查詢規格資訊 (內部 Feign 專用)", hidden = true)
    @PostMapping("/internal/variants/batch")
    public Result<List<VariantDTO>> getVariantsBatch(@RequestBody List<Long> ids) {
        return Result.ok(variantService.getVariantsByIds(ids));
    }

    /** 根據商品 ID 批量查詢所有規格（內部 Feign 專用）。 */
    @Operation(summary = "根據商品ID批量查詢所有規格 (內部 Feign 專用)", hidden = true)
    @PostMapping("/internal/variants/by-products")
    public Result<List<VariantDTO>> getVariantsByProductIds(@RequestBody List<Long> productIds) {
        return Result.ok(variantService.getVariantsByProductIds(productIds));
    }
}