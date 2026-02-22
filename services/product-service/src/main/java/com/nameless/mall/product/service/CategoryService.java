package com.nameless.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.product.api.dto.CategoryDTO;
import com.nameless.mall.product.entity.Category;

import java.util.List;

/**
 * 商品分類服務的接口
 * <p>
 * 繼承 IService<Category> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 */
public interface CategoryService extends IService<Category> {

    /**
     * 以樹狀結構，查詢所有商品分類
     * <p>
     * 這個方法會處理好父子分類之間的層級關係，
     * 回傳一個由 CategoryDTO 組成的、具有巢狀結構的列表，方便前端直接使用。
     *
     * @return 包含所有分類的樹狀結構列表
     */
    List<CategoryDTO> listWithTree();

    /**
     * 獲取分類路徑 (含所有父分類 ID)
     * 
     * @param categoryId 目標分類 ID
     * @return 從頂層到目標層的 ID 列表
     */
    List<Long> getCategoryPath(Long categoryId);

}
