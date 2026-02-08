package com.nameless.mall.order.api.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 透過 RabbitMQ 異步處理的特賣活動訊息
 * <p>
 * Payload 用於 Consumer 建單
 */
@Data
public class FlashSaleMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用戶 ID
     */
    private Long userId;

    /**
     * 活動 ID
     */
    private Long promotionId;

    /**
     * 商品 SKU ID
     */
    private Long skuId;

    /**
     * 購買數量
     */
    private Integer quantity;

    /**
     * 防重覆 Token (用來作為冪等鍵)
     */
    private String orderToken;

    /**
     * 下單時間戳 (用於排序或超時判定)
     */
    private Long timestamp;

    /**
     * 收件人姓名
     */
    private String receiverName;

    /**
     * 收件人電話
     */
    private String receiverPhone;

    /**
     * 收件人地址
     */
    private String receiverAddress;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 物流方式
     */
    private Integer shippingMethod;
}
