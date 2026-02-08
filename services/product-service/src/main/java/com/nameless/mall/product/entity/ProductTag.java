package com.nameless.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 商品-標籤關聯表 (Many-to-Many)
 * <p>
 * 對應資料庫中的 `product_tags` 表。
 * 用於記錄商品擁有的標籤 (如 ID=1商品 擁有 ID=2"HOT"標籤)。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("product_tags")
public class ProductTag extends Model<ProductTag> {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 標籤ID
     */
    private Long tagId;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;
}
