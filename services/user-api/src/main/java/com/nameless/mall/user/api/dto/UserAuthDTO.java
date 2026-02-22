package com.nameless.mall.user.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 專門用於服務間傳遞使用者認證資訊的 DTO
 */
@Data
public class UserAuthDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;

    private String password;
    private String email;
    private String nickname;
    private Integer status;
    private List<String> roles;
}