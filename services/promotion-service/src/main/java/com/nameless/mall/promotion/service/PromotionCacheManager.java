package com.nameless.mall.promotion.service;

import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.promotion.client.ProductClient;
import com.nameless.mall.promotion.config.CacheConfig;
import com.nameless.mall.core.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分類層級快取管理服務
 * 職責：管理分類樹的攤平映射，支持快速的父子級判定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionCacheManager {

    private final ProductClient productClient;

    /**
     * 獲取分類 ID 及其父 ID 的映射 Map
     * k: 分類ID, v: 父ID
     * 使用 Caffeine 緩存，sync=true 防止大併發下的緩存擊穿
     */
    @Cacheable(value = CacheConfig.CACHE_CATEGORY_TREE, sync = true)
    public Map<Long, Long> getCategoryParentMap() {
        log.info("【計價引擎】緩存未命中，正在重新加載分類樹資料...");
        Map<Long, Long> map = new HashMap<>();
        try {
            Result<List<CategoryDTO>> result = productClient.getCategoryTree();
            if (result != null && result.getData() != null) {
                flattenCategoryTree(result.getData(), null, map);
            }
        } catch (Exception e) {
            log.error("【計價引擎】遠端加載分類樹失敗", e);
        }
        return map;
    }

    private void flattenCategoryTree(List<CategoryDTO> nodes, Long parentId, Map<Long, Long> map) {
        if (nodes == null)
            return;
        for (CategoryDTO node : nodes) {
            if (parentId != null)
                map.put(node.getId(), parentId);
            if (node.getChildren() != null) {
                flattenCategoryTree(node.getChildren(), node.getId(), map);
            }
        }
    }
}
