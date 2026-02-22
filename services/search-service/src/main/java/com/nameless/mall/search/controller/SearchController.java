package com.nameless.mall.search.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 搜尋服務 Controller，提供基於 Elasticsearch 的商品搜尋與聖合篩選 API。 */
@Tag(name = "搜尋服務", description = "提供基於 Elasticsearch 的高效搜尋與篩選 API")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "商品高級搜尋 (含聚合篩選)")
    @GetMapping("/products")
    public Result<com.nameless.mall.search.api.vo.SearchResponseVO> searchProducts(
            com.nameless.mall.search.api.dto.SearchRequestDTO request) {
        com.nameless.mall.search.api.vo.SearchResponseVO result = searchService.searchAdvanced(request);
        return Result.ok(result);
    }

    @Operation(summary = "全量同步索引 (運維專用)", hidden = true)
    @PostMapping("/internal/sync-all")
    public Result<Integer> syncAll() {
        int count = searchService.syncAll();
        return Result.ok(count);
    }
}