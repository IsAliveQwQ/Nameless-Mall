package com.nameless.mall.search.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜尋結果展示對象 (Search Result View Object)
 * 用於解耦 ES 實體與前台展示
 */
@Data
public class ProductSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    /**
     * 副標題
     */
    private String title;

    private Long categoryId;
    private String categoryName;

    /**
     * 品牌名稱
     */
    private String brandName;

    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer salesCount;
    private Integer stock;

    /**
     * 主圖 URL
     */
    private String mainImage;

    /**
     * SKU 列表 (簡化)
     */
    private List<String> skus;

    private List<String> tags;
    private LocalDateTime publishedAt;
}
