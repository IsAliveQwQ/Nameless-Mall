package com.nameless.mall.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

/**
 * 使用者更新個人資料時，用來接收前端傳來資料的物件 (擴充版)
 * 所有欄位皆為可選，以便支援部分更新。
 */
@Data
public class UserUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Email(message = "Email 格式不正確")
    private String email;

    @Size(max = 20, message = "手機號碼長度不能超過 20 個字元")
    private String phone;

    @Size(max = 64, message = "暱稱長度不能超過 64 個字元")
    private String nickname;

    private String avatar;

    private String currentPassword;

    @Size(min = 6, max = 20, message = "新密碼長度必須在 6 到 20 個字元之間")
    private String newPassword;
}
