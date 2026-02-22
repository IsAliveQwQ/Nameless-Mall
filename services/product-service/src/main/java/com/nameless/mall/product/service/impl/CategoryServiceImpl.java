package com.nameless.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.entity.Category;
import com.nameless.mall.product.mapper.CategoryMapper;
import com.nameless.mall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分類服務的實現類
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private static final Long ROOT_PARENT_ID = 0L;

    /** 本地快取：分類樹幾乎不變，避免每次全表掃描 */
    private volatile List<CategoryDTO> cachedTree;
    private volatile long treeCacheTimestamp;
    private static final long TREE_CACHE_TTL_MS = 5 * 60 * 1000L; // 5 分鐘
    /** ID→ParentID 映射，供 getCategoryPath 使用（隨 tree 快取一起更新） */
    private volatile Map<Long, Long> categoryParentMap;

    /**
     * 獲取完整的分類樹狀結構
     * 
     * @return 樹狀分類列表
     */
    @Override
    public List<CategoryDTO> listWithTree() {
        // DCL 快取：避免高並發下重複全表掃描
        if (cachedTree != null && System.currentTimeMillis() - treeCacheTimestamp < TREE_CACHE_TTL_MS) {
            return cachedTree;
        }
        synchronized (this) {
            if (cachedTree != null && System.currentTimeMillis() - treeCacheTimestamp < TREE_CACHE_TTL_MS) {
                return cachedTree;
            }
            log.info("【分類快取】快取過期或不存在，重新讀取 DB");
            List<CategoryDTO> result = buildTreeFromDB();
            cachedTree = result;
            treeCacheTimestamp = System.currentTimeMillis();
            return result;
        }
    }

    private List<CategoryDTO> buildTreeFromDB() {
        // 1. 查詢出所有的商品分類
        List<Category> allCategories = baseMapper.selectList(null);

        // 建立 ID→ParentID 扁平映射，供 getCategoryPath 免查 DB
        this.categoryParentMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getParentId, (v1, v2) -> v1));

        // 2. 將所有的 Category 實體轉換為 CategoryDTO 物件
        List<CategoryDTO> allCategoryDTOs = allCategories.stream().map(category -> {
            CategoryDTO categoryDTO = new CategoryDTO();
            BeanUtils.copyProperties(category, categoryDTO);
            return categoryDTO;
        }).collect(Collectors.toList());

        // 3. 找到所有的一級分類 (頂層分類)
        List<CategoryDTO> level1Categories = allCategoryDTOs.stream()
                .filter(categoryDTO -> ROOT_PARENT_ID.equals(categoryDTO.getParentId()))
                .map(menu -> {
                    // 4. 為每一個一級分類，遞迴地設定它的子分類
                    menu.setChildren(getChildren(menu, allCategoryDTOs));
                    return menu;
                })
                .sorted(Comparator.comparingLong(CategoryDTO::getId))
                .collect(Collectors.toList());

        return level1Categories;
    }

    /** 清除分類樹快取（分類變更時呼叫） */
    public void evictTreeCache() {
        this.cachedTree = null;
        this.categoryParentMap = null;
        this.treeCacheTimestamp = 0;
    }

    /**
     * 遞迴輔助方法：從所有分類中，為指定的父分類尋找其所有子分類
     * 
     * @param root 當前的父分類
     * @param all  所有的分類 DTO 列表
     * @return 當前父分類的所有子分類（已組建成樹狀）
     */
    private List<CategoryDTO> getChildren(CategoryDTO root, List<CategoryDTO> all) {
        List<CategoryDTO> children = all.stream()
                .filter(categoryDTO -> {
                    // 根據 parentId 找到子分類
                    return categoryDTO.getParentId().equals(root.getId());
                })
                .map(categoryDTO -> {
                    // 為找到的子分類，繼續遞迴尋找它的子分類
                    categoryDTO.setChildren(getChildren(categoryDTO, all));
                    return categoryDTO;
                })
                .sorted(Comparator.comparingLong(CategoryDTO::getId))
                .collect(Collectors.toList());
        return children;
    }

    /**
     * 獲取指定分類的完整路徑編號 (頂層 -> 當前)
     * 
     * @param categoryId 目標分類 ID
     * @return 分類 ID 路徑列表
     */
    @Override
    public List<Long> getCategoryPath(Long categoryId) {
        // 優先使用記憶體快取，避免遞迴逐層查 DB
        Map<Long, Long> parentMap = this.categoryParentMap;
        if (parentMap != null && !parentMap.isEmpty()) {
            List<Long> path = new ArrayList<>();
            Long current = categoryId;
            while (current != null && !ROOT_PARENT_ID.equals(current)) {
                path.add(current);
                current = parentMap.get(current);
            }
            Collections.reverse(path);
            return path;
        }
        // 回退：快取未建立時仍使用原始 DB 遞迴
        List<Long> path = new ArrayList<>();
        findPath(categoryId, path);
        // 原本順序是從子到父，反轉為從頂層到基層 (例如: [1, 11, 111])
        Collections.reverse(path);
        return path;
    }

    /**
     * 遞迴查找父節點並構建路徑
     */
    private void findPath(Long categoryId, List<Long> path) {
        if (categoryId == null || ROOT_PARENT_ID.equals(categoryId))
            return;
        path.add(categoryId);
        Category category = baseMapper.selectById(categoryId);
        if (category != null && category.getParentId() != null && !ROOT_PARENT_ID.equals(category.getParentId())) {
            findPath(category.getParentId(), path);
        }
    }
}
