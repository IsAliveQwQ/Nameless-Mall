package com.nameless.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單主表實體類
 * <p>
 * 對應資料庫中的 `orders` 表。
 */
@Data
@TableName("orders")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 訂單唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 訂單的業務編號 (唯一)
     */
    private String orderSn;

    /**
     * 下單使用者的ID
     */
    private Long userId;

    /**
     * 訂單總金額
     */
    private BigDecimal totalAmount;

    /**
     * 實際支付金額
     */
    private BigDecimal payAmount;

    /**
     * 優惠金額
     */
    private BigDecimal discountAmount;

    /**
     * 運費
     */
    private BigDecimal shippingFee;

    /**
     * 訂單狀態 (0:待付款; 1:處理中; 2:已出貨; 3:已完成; 4:已取消; 5:建立中; 6:建立失敗)
     * @see com.nameless.mall.order.api.enums.OrderStatus
     */
    private Integer status;

    /**
     * 支付方式 (1:銀行轉帳, 2:貨到付款)
     */
    private Integer payType;

    /**
     * 配送方式 (1:宅配, 2:超商取貨)
     */
    private Integer shippingMethod;

    /**
     * 支付帳戶資訊 (例如: 銀行轉帳的後五碼)
     */
    private String paymentAccountInfo;

    /**
     * 訂單備註
     */
    private String note;

    /**
     * 建立失敗原因（異步下單用）
     */
    private String failReason;

    /**
     * 使用的優惠券 ID
     */
    private Long userCouponId;

    /**
     * 付款時間
     */
    private LocalDateTime paidAt;

    /**
     * 邏輯刪除標記 (0:正常, 1:已刪除)
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}
