package com.nameless.mall.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 使用者註冊時，用來接收前端傳來資料的物件
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "使用者名稱不能為空")
    @Size(min = 4, max = 20, message = "使用者名稱長度必須在 4 到 20 個字元之間")
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, max = 20, message = "密碼長度必須在 6 到 20 個字元之間")
    private String password;

    @Email(message = "Email 格式不正確")
    private String email;
}