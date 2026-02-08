package com.nameless.mall.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 限時特賣商品關聯實體類
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("flash_sale_skus")
public class FlashSaleSku implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主鍵 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 特賣活動 ID
     */
    private Long promotionId;

    /**
     * 商品 SPU ID
     */
    private Long productId;

    /**
     * 商品規格 SKU ID
     */
    private Long variantId;

    /**
     * 原價 (計算基準)
     */
    private BigDecimal originalPrice;

    /**
     * 特賣價格
     */
    private BigDecimal flashSalePrice;

    /**
     * 特賣庫存
     */
    private Integer flashSaleStock;

    /**
     * 特賣總量限制
     */
    private Integer flashSaleLimit;

    /**
     * 每人限購數量
     */
    private Integer limitPerUser;

    /**
     * 已售數量
     */
    private Integer soldCount;
}
