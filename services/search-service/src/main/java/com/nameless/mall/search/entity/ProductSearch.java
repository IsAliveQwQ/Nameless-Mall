package com.nameless.mall.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ES 商品索引文檔 (增強版)
 * 對應名為 "products" 的索引
 */
@Data
@Document(indexName = "products")
public class ProductSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    /**
     * 商品名稱 (IK 分詞)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /**
     * 商品副標題/賣點 (IK 分詞)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 描述 (IK 分詞)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 商品分類 ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 商品分類名稱 (Keyword)
     */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /**
     * 分類層級 ID 列表 (包含父分類)
     * 用於實現「選中父分類，顯示其下所有子分類商品」
     */
    @Field(type = FieldType.Long)
    private List<Long> categoryHierarchy;

    /**
     * 品牌名稱
     */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /**
     * 價格
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 商品原價 (用於搜尋列表顯示折扣)
     */
    @Field(type = FieldType.Double)
    private BigDecimal originalPrice;

    /**
     * 銷量
     */
    @Field(type = FieldType.Integer)
    private Integer salesCount;

    /**
     * 庫存
     */
    @Field(type = FieldType.Integer)
    private Integer stock;

    /**
     * 主圖 URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String mainImage;

    /**
     * 標籤列表 (Keyword)
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * SKU 規格列表 (Keyword)
     */
    @Field(type = FieldType.Keyword)
    private List<String> skus;

    /**
     * 上架時間 (用於排序)
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime publishedAt;

    /**
     * 商品規格屬性 (Nested 型別以支援精準檢索與聚合)
     */
    @Field(type = FieldType.Nested)
    private List<AttributeValue> attrs;

    @Data
    public static class AttributeValue implements Serializable {
        @Field(type = FieldType.Long)
        private Long attrId;
        @Field(type = FieldType.Keyword)
        private String attrName;
        @Field(type = FieldType.Keyword)
        private String attrValue;
    }
}
