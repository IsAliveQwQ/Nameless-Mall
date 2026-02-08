package com.nameless.mall.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用戶收貨地址實體類
 * <p>
 * 對應資料庫中的 `user_addresses` 表。
 */
@Data
@TableName("user_addresses")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主鍵 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用戶 ID
     */
    private Long userId;

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
     * 詳細地址
     */
    private String detailAddress;

    /**
     * 郵遞區號
     */
    private String postalCode;

    /**
     * 是否為預設地址 (0:否, 1:是)
     */
    private Integer isDefault;

    /**
     * 標籤 (家/公司/學校)
     */
    private String tag;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}
