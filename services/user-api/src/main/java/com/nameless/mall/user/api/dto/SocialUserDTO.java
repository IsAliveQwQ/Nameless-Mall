package com.nameless.mall.user.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SocialUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String provider; // 例如 "google"
    private String providerId; // 使用者在 Google 的唯一 ID
    private String username; // 使用者在 Google 的顯示名稱
    private String email; // 使用者在 Google 的 Email
    private String attributes; // 儲存從 Google 獲取的完整屬性 JSON 字串
}