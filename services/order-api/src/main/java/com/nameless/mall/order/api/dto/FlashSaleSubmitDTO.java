package com.nameless.mall.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 特賣活動請求 DTO
 * <p>
 * 專為高併發特賣設計，僅包含最核心的必要參數。
 */
@Data
public class FlashSaleSubmitDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 特賣活動 ID
     */
    @NotNull(message = "活動 ID 不能為空")
    private Long promotionId;

    /**
     * 商品 SKU ID (Variant ID)
     */
    @NotNull(message = "商品 ID 不能為空")
    private Long skuId;

    /**
     * 購買數量
     */
    @Min(value = 1, message = "購買數量至少為 1")
    private Integer quantity;

    /**
     * 防重覆 Token
     */
    private String orderToken;

    /**
     * 收件人姓名
     */
    @NotEmpty(message = "收件人姓名不能為空")
    private String receiverName;

    /**
     * 收件人電話
     */
    @NotEmpty(message = "收件人電話不能為空")
    private String receiverPhone;

    /**
     * 收件人地址
     */
    @NotEmpty(message = "收件人地址不能為空")
    private String receiverAddress;

    /**
     * 支付方式 (例如: 1=信用卡, 2=LinePay)
     */
    @NotNull(message = "支付方式不能為空")
    private Integer payType;

    /**
     * 物流方式 (例如: 1=宅配, 2=超取)
     */
    @NotNull(message = "物流方式不能為空")
    private Integer shippingMethod;
}
