package com.nameless.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品規格的具體選項值實體類
 * <p>
 * 對應資料庫中的 `variant_options` 表。
 * 例如，某個 Variant (SKU) 對應到這裡的兩筆紀錄：
 * 1. (optionName: "顏色", optionValue: "紅色")
 * 2. (optionName: "尺寸", optionValue: "L")
 */
@Data
@TableName("variant_options")
public class VariantOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 選項唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所屬規格ID (外鍵, 對應 variants.id)
     */
    private Long variantId;

    /**
     * 選項類型 (例如 "顏色", "尺寸", "容量")
     */
    private String optionName;

    /** 選項值 (例如 "紅色", "L", "256GB") */
    private String optionValue;

    /**
     * 邏輯刪除 (0:正常, 1:已刪除)
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
