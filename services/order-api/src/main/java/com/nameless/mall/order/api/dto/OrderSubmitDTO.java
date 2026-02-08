package com.nameless.mall.order.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 提交訂單時，從前端接收的資料傳輸物件 (DTO)
 */
@Data
public class OrderSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 要結帳的購物車項目 ID 列表，據此從購物車服務取得商品資訊。
     */
    @NotEmpty(message = "結帳商品不能為空")
    private List<Long> cartItemIds;

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
     * 支付方式 (1:LINE Pay, 2:綠界金流)
     */
    @NotNull(message = "請選擇支付方式")
    private Integer payType;

    /**
     * 配送方式 (1:宅配, 2:7-11取貨付款)
     */
    @NotNull(message = "請選擇配送方式")
    private Integer shippingMethod;

    /**
     * 銀行轉帳的後五碼 (可選)
     */
    private String paymentAccountInfo;

    /**
     * 防重覆提交的唯一權杖 (由後端在進入結帳頁時產生)
     */
    @NotEmpty(message = "訂單權杖無效")
    private String orderToken;

    /**
     * 使用的優惠券 ID (可選)
     * 若用戶選擇使用優惠券，則傳入用戶優惠券的 ID
     */
    private Long userCouponId;

    /**
     * 訂單備註 (可選)
     */
    private String note;
}
