package com.nameless.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 優惠券範本實體類
 */
@Data
@TableName("coupon_templates")
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 優惠券名稱 */
    private String name;

    /** 使用說明 */
    private String description;

    /** 類型: 1=滿減 2=折扣 3=免運 */
    private Integer type;

    /** 使用門檻 (滿X元可用) */
    private BigDecimal threshold;

    /** 優惠金額或折扣比例 */
    private BigDecimal discount;

    /** 最大折扣金額 (用於折扣券) */
    private BigDecimal maxDiscount;

    /** 發放總量 */
    private Integer totalCount;

    /** 剩餘數量 */
    private Integer remainCount;

    /** 每人限領數量 */
    private Integer perUserLimit;

    /** 領取開始時間 */
    private LocalDateTime startTime;

    /** 領取結束時間 */
    private LocalDateTime endTime;

    /** 有效期類型: 1=固定日期, 2=領取後有效 */
    private Integer validType;

    /** 領取後有效天數 (僅 validType=2 有效) */
    private Integer validDays;

    /** 狀態: 0=停用 1=啟用 */
    private Integer status;

    /** 邏輯刪除標記 (0:正常, 1:已刪除) */
    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
