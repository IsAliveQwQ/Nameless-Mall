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
 * 商品規格庫存實體類
 * <p>
 * 對應資料庫中的 `variants` 表。
 */
@Data
@TableName("variants")
public class Variant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 規格唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所屬商品ID (外鍵)
     */
    private Long productId;

    /**
     * 規格名稱 (如: 黑色 128G)
     */
    private String name;

    /**
     * 規格圖片 URL
     */
    private String image;

    /**
     * 庫存單位編碼
     */
    private String sku;

    /**
     * 此規格的價格
     */
    private BigDecimal price;

    /**
     * 此規格的原價/定價
     */
    private BigDecimal originalPrice;

    /**
     * 此規格的庫存
     */
    private Integer stock;

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
