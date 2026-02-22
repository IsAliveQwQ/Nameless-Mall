package com.nameless.mall.auth.controller;

import com.nameless.mall.auth.api.vo.LoginResultVO;
import com.nameless.mall.auth.service.JwtTokenService;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認證與授權 Controller。
 * <p>
 * 處理用戶登入與第三方 OAuth2 授權。
 * 登入驗證委由 Spring Security
 * {@link org.springframework.security.authentication.AuthenticationManager} 處理。
 * 驗證成功後簽發的 JWT 僅包含必要角色與 UserID，以控制 Token 體積。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    /** JWT 過期時間（毫秒），從配置讀取，避免硬編碼 */
    private final long jwtExpirationMs;

    public AuthController(AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @Data
    static class LoginRequest {
        @NotBlank(message = "使用者名稱不得為空")
        private String username;
        @NotBlank(message = "密碼不得為空")
        private String password;
    }

    /**
     * 處理用戶帳號密碼登入請求。
     * <p>
     * 使用 Spring Security 的 AuthenticationManager 進行驗證。
     * 若驗證成功，由 JwtTokenService 簽發具有時效性的 Access Token。
     * </p>
     *
     * @param loginRequest 封裝帳號密碼的 DTO，並透過 @Valid 確保不為空
     * @return 封裝登入成功結果 (Token 與基本用戶資訊) 的 Result 物件
     * @throws BusinessException 當帳密錯誤時拋出 CREDENTIALS_INVALID (HTTP 401)
     */
    @PostMapping("/login")
    public Result<LoginResultVO> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 執行 Spring Security 認證
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            // 產生 JWT Token 並組裝回應
            String jwt = jwtTokenService.generateToken(authentication);
            LoginResultVO data = buildLoginResult(jwt, authentication);

            return Result.ok(data, "登入成功");
        } catch (AuthenticationException e) {
            log.warn("登入認證失敗: {}, 原因: {}", loginRequest.getUsername(), e.getMessage());
            throw new BusinessException(ResultCodeEnum.CREDENTIALS_INVALID);
        }
        // 其他 Exception 由 GlobalExceptionHandler 統一處理
    }

    /**
     * 組裝登入成功回應。
     */
    private LoginResultVO buildLoginResult(String jwt, Authentication authentication) {
        return LoginResultVO.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(buildUserInfo(authentication))
                .build();
    }

    /**
     * 從認證結果中提取用戶資訊。
     */
    private LoginResultVO.UserInfo buildUserInfo(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof com.nameless.mall.auth.entity.SecurityUser userDetails) {
            return LoginResultVO.UserInfo.builder()
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .roles(userDetails.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList())
                    .email(userDetails.getEmail())
                    .nickname(userDetails.getNickname())
                    .build();
        }

        // Fallback: 當 Principal 未依預期實作 SecurityUser 時的安全降級
        return LoginResultVO.UserInfo.builder()
                .username(authentication.getName())
                .build();
    }
}
