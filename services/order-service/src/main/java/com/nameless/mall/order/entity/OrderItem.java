package com.nameless.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 訂單項目實體類
 * <p>
 * 對應資料庫中的 `order_items` 表。
 * 儲存了交易發生當下的商品資訊快照。
 */
@Data
@TableName("order_items")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 訂單項目唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所屬訂單ID (外鍵, 對應 orders.id)
     */
    private Long orderId;

    /**
     * 所屬訂單的業務編號 (冗餘欄位, 方便查詢)
     */
    private String orderSn;

    /**
     * 商品ID (SPU ID)
     */
    private Long productId;

    /**
     * 商品規格ID (SKU ID)
     */
    private Long variantId;

    /**
     * 商品名稱 (交易快照)
     */
    private String productName;

    /**
     * 商品圖片 (交易快照)
     */
    private String productImage;

    /**
     * 商品單價 (交易快照)
     */
    private BigDecimal productPrice;

    /**
     * 規格名稱 (交易快照, 如: 黑色 128G)
     */
    private String skuName;

    /**
     * 購買數量
     */
    private Integer quantity;

    /**
     * 商品原價 (交易快照，促銷前的價格)
     */
    private BigDecimal originalPrice;

    /**
     * 促銷活動名稱 (交易快照，如: 夏季特賣)
     */
    private String promotionName;

    /**
     * 促銷折扣金額 (單件商品的折扣額)
     */
    private BigDecimal promotionAmount;
}
