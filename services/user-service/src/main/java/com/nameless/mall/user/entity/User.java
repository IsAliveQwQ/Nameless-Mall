package com.nameless.mall.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 使用者實體類，嚴格對應資料庫中的 `users` 表結構。
 * <p>
 * 這是 user-service 的內部私有檔案，不會暴露給其他服務。
 */
@Data
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主鍵ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 使用者名稱
     */
    private String username;

    /**
     * 加密後的密碼
     */
    private String password;

    /**
     * 電子郵件
     */
    private String email;

    /**
     * 手機號碼
     */
    private String phone;

    /**
     * 暱稱
     */
    private String nickname;

    /**
     * 頭像 URL
     */
    private String avatar;

    /**
     * 狀態 (0:正常, 1:停用)
     */
    private Integer status;

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
