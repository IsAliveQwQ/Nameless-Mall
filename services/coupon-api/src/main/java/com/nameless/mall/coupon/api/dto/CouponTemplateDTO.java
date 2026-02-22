package com.nameless.mall.coupon.api.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 優惠券範本 DTO
 */
@Data
public class CouponTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** 剩餘數量 */
    private Integer remainCount;

    /** 每人限領數量 */
    private Integer perUserLimit;

    /** 有效期開始 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /** 有效期結束 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /** 狀態: 0=停用 1=啟用 */
    private Integer status;

    /** 有效期類型: 1=固定日期, 2=領取後有效 */
    private Integer validType;

    /** 領取後有效天數 (僅 validType=2 有效) */
    private Integer validDays;
}
