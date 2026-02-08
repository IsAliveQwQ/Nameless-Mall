package com.nameless.mall.search.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "搜尋服務後台", description = "同步、維運等私有 API")
@RestController
@RequestMapping("/search/admin")
@RequiredArgsConstructor
public class AdminSearchController {

    private final SearchService searchService;

    @Operation(summary = "全量同步 MySQL 商品資料到 Elasticsearch")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync")
    public Result<String> syncAll() {
        int count = searchService.syncAll();
        return Result.ok("同步成功，共計 " + count + " 筆資料。");
    }
}
