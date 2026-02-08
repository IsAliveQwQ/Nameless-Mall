package com.nameless.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用戶優惠券實體類
 */
@Data
@TableName("user_coupons")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long templateId;

    /** 狀態: 0=未使用 1=已使用 2=已過期 */
    private Integer status;

    /** 使用時的訂單號 */
    private String orderSn;

    /** 有效期開始 (快照) */
    private LocalDateTime validStartTime;

    /** 有效期結束 (快照) */
    private LocalDateTime validEndTime;

    /** 領取時間 */
    private LocalDateTime obtainedAt;

    /** 使用時間 */
    private LocalDateTime usedAt;

    private Integer isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
