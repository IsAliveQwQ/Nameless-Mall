package com.nameless.mall.product.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品資料傳輸物件 - 用於列表或概要展示
 * <p>
 * 這個 DTO 只包含商品的核心概要資訊，適合在商品列表、搜尋結果等場景下回傳給前端。
 * 它省略了較大的欄位，如 description，以提高傳輸效率。
 */
@Data
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 商品基礎價格
     */
    private BigDecimal price;

    /**
     * 商品原價/定價 (用於顯示劃線價格)
     */
    private BigDecimal originalPrice;

    /**
     * 商品庫存數量
     */
    private Integer stock;

    /**
     * 商品所屬分類的ID
     */
    private Long categoryId;

    /**
     * 商品所屬分類的名稱 (冗餘欄位，方便前端直接顯示)
     */
    private String categoryName;

    /**
     * 商品主圖 (對齊前端 API 規範)
     */
    private String mainImage;

    /**
     * 商品標籤 (如 ["NEW", "HOT"])
     */
    private List<String> tags;

    /**
     * 銷量
     */
    private Integer sales;

    /**
     * 上架時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    /**
     * 商品建立時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
