package com.nameless.mall.product.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 商品分類資料傳輸物件
 * <p>
 * 用於在 Controller 層回傳給前端，或在服務間進行傳遞。
 * 這個 DTO 設計為可以表示一個樹狀結構。
 */
@Data
public class CategoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分類唯一ID
     */
    private Long id;

    /**
     * 分類名稱
     */
    private String name;

    /**
     * 父分類ID
     */
    private Long parentId;

    /**
     * 分類層級
     */
    private Integer level;

    /**
     * 分類圖標 URL
     */
    private String icon;

    /**
     * 排序權重
     */
    private Integer sortOrder;

    /**
     * 狀態 (1:啟用, 0:停用)
     */
    private Integer status;

    /**
     * 子分類列表，用於構建分類樹
     */
    private List<CategoryDTO> children;
}
