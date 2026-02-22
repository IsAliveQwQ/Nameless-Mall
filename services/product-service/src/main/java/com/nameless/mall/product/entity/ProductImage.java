package com.nameless.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 商品圖片實體類
 * <p>
 * 對應資料庫中的 `product_images` 表，
 * 透過 product_id 與 Product 實體建立一對多關聯。
 */
@Data
@TableName("product_images")
public class ProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 圖片唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所屬商品ID (外鍵, 對應 products.id)
     */
    private Long productId;

    /**
     * 圖片的 URL 位址
     */
    private String url;

    /**
     * 是否為主圖
     */
    private Boolean isMain;

    /**
     * 圖片的顯示順序，數字越小越靠前
     */
    private Integer sortOrder;
}
