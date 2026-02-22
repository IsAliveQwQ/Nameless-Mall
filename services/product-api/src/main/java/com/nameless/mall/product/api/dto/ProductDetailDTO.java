package com.nameless.mall.product.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品詳細資訊資料傳輸物件
 * <p>
 * 這是一個複合式的 DTO，用於商品詳細頁面，一次性提供前端所需的所有資訊。
 * 它聚合了商品主資訊、分類資訊、以及所有可選的規格列表。
 */
@Data
public class ProductDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 商品主資訊

    /**
     * 商品唯一ID
     */
    private Long id;

    /**
     * 商品名稱
     */
    private String name;

    /**
     * 商品副標題/摘要
     */
    private String title;

    /**
     * 商品詳細描述 (在詳細頁中需要完整顯示)
     */
    private String description;

    /**
     * 商品的基礎價格 (作為預設或參考價格)
     */
    private BigDecimal price;

    /**
     * 商品原價/定價 (用於顯示劃線價格)
     */
    private BigDecimal originalPrice;

    // 分類資訊

    /**
     * 商品所屬分類的ID
     */
    private Long categoryId;

    /**
     * 商品所屬分類的名稱 (冗餘欄位，方便前端直接顯示)
     */
    private String categoryName;

    // 規格與多媒體

    /**
     * 商品的所有可選規格列表
     */
    private List<VariantDTO> variants;

    /**
     * 商品的圖片 URL 列表 (例如：輪播圖)
     */
    private List<String> images;

    /**
     * 規格選項聚集表 (供前端渲染)
     */
    private Map<String, List<String>> displayOptions;

    // 其他資訊

    /**
     * 商品標籤 (如 ["NEW", "HOT"])
     */
    private List<String> tags;

    /**
     * 上架時間 (用於新品判斷)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    /**
     * 商品建立時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
