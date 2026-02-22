package com.nameless.mall.order.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 訂單詳情的視圖對象 (VO)
 */
@Data
public class OrderDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderSn;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private Integer status;
    private String statusName;
    private Integer payType;
    private String payTypeName;
    private Integer shippingMethod;
    private String shippingMethodName;
    private String paymentAccountInfo;
    private String note;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    private List<OrderItemVO> items;
    private ShipmentVO shipment;

    @Data
    public static class OrderItemVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long productId;
        private Long variantId;
        private String productVariantName; // 規格名稱 (如顏色/尺寸)
        private String productName;
        private String productImage;
        private BigDecimal productPrice;
        private Integer quantity;
        // 促銷快照欄位
        private BigDecimal originalPrice;
        private String promotionName;
        private BigDecimal promotionAmount;
    }

    @Data
    public static class ShipmentVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String trackingNumber;
        private LocalDateTime shippedAt;
    }
}
