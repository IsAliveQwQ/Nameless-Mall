package com.nameless.mall.order.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 訂單完整詳情的資料傳輸物件 (DTO)
 * <p>
 * 用於從後端回傳給前端，展示特定訂單的所有相關資訊。
 */
@Data
public class OrderDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 訂單主資訊
    private Long id;
    private String orderSn;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private Integer status;
    private Integer payType;
    private Integer shippingMethod;
    private String paymentAccountInfo;
    private String note;
    private Long userCouponId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 訂單項目列表
    private List<OrderItemDTO> items;

    // 運送資訊
    private ShipmentDTO shipment;

    /**
     * 運送資訊內部 DTO
     */
    @Data
    public static class ShipmentDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer shippingMethod;
        private BigDecimal shippingFee;
        private String carrier;
        private String receiverName;
        private String receiverPhone;
        private String province;
        private String city;
        private String district;
        private String receiverAddress;
        private String postalCode;

        // 超商取貨相關
        private Integer storeType;
        private String storeCode;
        private String storeName;
        private String storeAddress;
        private String pickupCode;

        private String trackingNumber;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate estimatedDelivery;
        private LocalDateTime shippedAt;
        private LocalDateTime receivedAt;
        private String deliveryNote;
    }
}
