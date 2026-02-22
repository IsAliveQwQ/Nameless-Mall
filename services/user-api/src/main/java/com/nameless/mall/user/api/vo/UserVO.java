package com.nameless.mall.user.api.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回傳給前端的視圖物件 (View Object)
 */
@Data
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
