package com.nameless.mall.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

/**
 * 密碼修改 DTO
 */
@Data
public class UserPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "舊密碼不能為空")
    private String oldPassword;

    @NotBlank(message = "新密碼不能為空")
    @Size(min = 6, max = 20, message = "密碼長度必須在 6 到 20 個字元之間")
    private String newPassword;
}
