package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品詳情視圖物件
 * <p>
 * 專為前端詳情頁渲染設計，包含：
 * 1. 核心資訊 (標題、價格、銷量)
 * 2. 視覺資訊 (多圖、標籤)
 * 3. 規格選擇器
 * 4. 完整的變體列表 (用於計算庫存和價格)
 */
@Data
public class ProductDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    /**
     * 副標題 (行銷賣點)
     */
    private String title;

    private String description;
    private BigDecimal price;

    /**
     * 商品原價 (用於劃線價格)
     */
    private BigDecimal originalPrice;

    /**
     * 總庫存 (後端計算後供參考，實際庫存看 variants)
     */
    private Integer stock;

    private Integer sales;

    /**
     * 商品主圖
     */
    private String mainImage;

    /**
     * 輪播圖列表
     */
    private List<String> images;

    /**
     * 商品標籤 (如 ["NEW", "HOT"])
     */
    private List<String> tags;

    /**
     * 規格選項表 (供前端渲染按鈕)
     * Key: 選項名 (如 "顏色"), Value: 選項值列表 (["星鈦色", "黑色"])
     */
    private Map<String, List<String>> displayOptions;

    /**
     * 完整的變體列表
     * 前端根據 displayOptions 選擇後，比對此列表找到對應的價格與庫存
     */
    private List<VariantVO> variants;

    private Long categoryId;
    private String categoryName;

    /**
     * 分類路徑 ID 列表 (包含所有父分類 ID)
     * 用於搜尋引擎實現「選中父分類，顯示子分類商品」
     */
    private List<Long> categoryHierarchy;

    /**
     * 上架時間 (可用於顯示新品標籤)
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    /**
     * 建立時間
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
