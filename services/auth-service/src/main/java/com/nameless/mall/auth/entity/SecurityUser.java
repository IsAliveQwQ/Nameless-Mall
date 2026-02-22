package com.nameless.mall.auth.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 專門用於 Spring Security 內部的使用者物件。
 * 它的職責是將從 UserAuthDTO 傳來的資料，適配成 Security 框架需要的格式。
 * 這個類別與 user-service 的 User Entity 完全無關。
 */
@Getter
@Setter
public class SecurityUser implements UserDetails {

    private Long id; // 使用者的數字 ID
    private String username;
    private String password;
    private Integer status;
    private String email;
    private String nickname;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != null && this.status == 0;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 安全的 toString，排除 password 欄位以防止密碼洩漏至日誌。
     */
    @Override
    public String toString() {
        return "SecurityUser{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}