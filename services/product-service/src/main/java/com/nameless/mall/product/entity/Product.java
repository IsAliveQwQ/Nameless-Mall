package com.nameless.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品主體實體類 (SPU - Standard Product Unit)
 * <p>
 * 對應資料庫中的 `products` 表。
 */
@Data
@TableName("products")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名稱
     */
    private String name;

    /**
     * 商品詳細描述
     */
    private String description;

    /**
     * 商品副標題/摘要
     */
    private String title;

    /**
     * 商品基礎價格
     */
    private BigDecimal price;

    /**
     * 商品原價/定價 (用於前端顯示劃線價格)
     */
    private BigDecimal originalPrice;

    /**
     * 商品總庫存
     */
    private Integer stock;

    /**
     * 上架時間 (用於新品排序)
     */
    private LocalDateTime publishedAt;

    /**
     * 所屬分類ID (外鍵)
     */
    private Long categoryId;

    /**
     * 商品主圖 URL
     */
    private String mainImage;

    /**
     * 商品狀態 (1:上架, 0:下架)
     */
    private Integer status;

    /**
     * 銷量
     */
    private Integer sales;

    /**
     * 邏輯刪除標記 (0:正常, 1:已刪除)
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}
