package com.nameless.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 訂單運送資訊實體類
 * <p>
 * 對應資料庫中的 `order_shipments` 表。
 */
@Data
@TableName("order_shipments")
public class OrderShipment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 運送資訊唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 對應的訂單ID (外鍵)
     */
    private Long orderId;

    /**
     * 對應的訂單業務編號
     */
    private String orderSn;

    /**
     * 配送方式 (1:宅配, 2:超商取貨)
     */
    private Integer shippingMethod;

    /**
     * 運費
     */
    private BigDecimal shippingFee;

    /**
     * 物流公司 (黑貓/新竹/宅配通)
     */
    private String carrier;

    /**
     * 收件人姓名
     */
    private String receiverName;

    /**
     * 收件人電話
     */
    private String receiverPhone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 區
     */
    private String district;

    /**
     * 收件人完整地址
     */
    private String receiverAddress;

    /**
     * 郵遞區號
     */
    private String postalCode;

    /**
     * 超商類型 (1:7-11, 2:全家, 3:萊爾富, 4:OK)
     */
    private Integer storeType;

    /**
     * 門市代碼
     */
    private String storeCode;

    /**
     * 門市名稱
     */
    private String storeName;

    /**
     * 門市地址
     */
    private String storeAddress;

    /**
     * 取貨碼
     */
    private String pickupCode;

    /**
     * 貨運追蹤單號
     */
    private String trackingNumber;

    /**
     * 預計送達日期
     */
    private LocalDate estimatedDelivery;

    /**
     * 出貨時間
     */
    private LocalDateTime shippedAt;

    /**
     * 簽收時間
     */
    private LocalDateTime receivedAt;

    /**
     * 配送備註
     */
    private String deliveryNote;
}

