package com.nameless.mall.auth.api.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 登入成功後的回應物件 (Value Object)
 */
@Data
@Builder
public class LoginResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo implements Serializable {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private List<String> roles;
    }
}
