package com.nameless.mall.product.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.api.vo.CategoryVO;
import com.nameless.mall.product.service.CategoryService;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分類的 Controller
 */
@Tag(name = "商品分類管理", description = "提供商品分類查詢等 API")
@RestController
@RequestMapping("/products/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查詢所有商品分類，並以樹狀結構回傳
     *
     * @return 包含所有分類的樹狀結構列表
     */
    @Operation(summary = "查詢樹狀分類列表")
    @GetMapping("/tree")
    public Result<List<CategoryVO>> listWithTree() {
        List<CategoryDTO> categoryTree = categoryService.listWithTree();
        return Result.ok(toVOs(categoryTree));
    }

    @Operation(summary = "Internal: 查詢樹狀分類列表", hidden = true)
    @GetMapping("/internal/tree")
    public Result<List<CategoryDTO>> listWithTreeInternal() {
        return Result.ok(categoryService.listWithTree());
    }

    private List<CategoryVO> toVOs(List<CategoryDTO> dtos) {
        if (dtos == null)
            return null;
        return dtos.stream().map(this::toVO).collect(Collectors.toList());
    }

    private CategoryVO toVO(CategoryDTO dto) {
        if (dto == null)
            return null;
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(dto, vo, "children");
        if (dto.getChildren() != null) {
            vo.setChildren(toVOs(dto.getChildren()));
        }
        return vo;
    }
}
