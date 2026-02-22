package com.nameless.mall.auth.handler;

import com.nameless.mall.auth.feign.UserServiceFeignClient;
import com.nameless.mall.auth.service.JwtTokenService;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.user.api.dto.SocialUserDTO;
import com.nameless.mall.user.api.dto.UserAuthDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserServiceFeignClient userServiceFeignClient;
    private final JwtTokenService jwtTokenService;

    @Value("${app.frontend.callback-url:http://localhost:3000/login/callback}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 登入成功，處理使用者建檔與 JWT 簽發");

        try {
            // 1. 提取 OAuth2 用戶資訊
            OAuth2UserInfo userInfo = extractOAuth2UserInfo(authentication);

            // 2. 透過 Feign 查找或建立本地用戶
            UserAuthDTO internalUser = findOrCreateLocalUser(userInfo);

            // 3. 建立內部認證物件並生成 JWT
            String jwtToken = generateJwtForUser(internalUser);

            // 4. 重導向到前端
            redirectWithToken(response, jwtToken, internalUser.getUsername());

        } catch (Exception e) {
            handleAuthenticationError(response, e);
        }
    }

    /**
     * 從 OAuth2 認證結果中提取用戶資訊。
     */
    private OAuth2UserInfo extractOAuth2UserInfo(Authentication authentication) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = token.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub"); // Google 的唯一 ID

        log.info("Google 使用者資訊 - Email: {}, Name: {}, ID: {}", email, name, providerId);

        return new OAuth2UserInfo(email, name, providerId);
    }

    /**
     * 透過 Feign 查找或建立本地用戶。
     */
    private UserAuthDTO findOrCreateLocalUser(OAuth2UserInfo userInfo) {
        // 建立 SocialUserDTO
        SocialUserDTO socialUserDTO = new SocialUserDTO();
        socialUserDTO.setEmail(userInfo.email());
        // [防禦] 避免 Google Name 與現有 Username 衝突，加上隨機後綴
        String safeUsername = userInfo.name() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        socialUserDTO.setUsername(safeUsername);
        socialUserDTO.setProviderId(userInfo.providerId());
        socialUserDTO.setProvider("GOOGLE");

        Result<UserAuthDTO> result = userServiceFeignClient.findOrCreateBySocial(socialUserDTO);
        UserAuthDTO internalUser = (result != null && result.isSuccess()) ? result.getData() : null;

        if (internalUser == null) {
            log.error("user-service 回傳失敗或不可用，無法建立或查找用戶: email={}, result={}",
                    userInfo.email(), result);
            throw new BusinessException(ResultCodeEnum.SERVICE_UNAVAILABLE,
                    "用戶服務不可用或回傳資料異常");
        }

        log.info("從 user-service 找到或建立了本地使用者: {}", internalUser.getUsername());
        return internalUser;
    }

    /**
     * 建立內部認證物件並生成 JWT Token。
     */
    private String generateJwtForUser(UserAuthDTO internalUser) {
        // 建立 authorities
        Collection<GrantedAuthority> authorities;
        if (internalUser.getRoles() != null && !internalUser.getRoles().isEmpty()) {
            authorities = internalUser.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        } else {
            // 預設角色
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Principal 使用 userId，JWT subject 需為可解析的 Long
        Authentication internalAuth = new UsernamePasswordAuthenticationToken(
                String.valueOf(internalUser.getId()),
                null,
                authorities);

        String jwtToken = jwtTokenService.generateToken(internalAuth);
        log.info("成功生成 JWT Token 給使用者: {}", internalUser.getUsername());

        return jwtToken;
    }

    /**
     * 重導向到前端（成功）。
     */
    private void redirectWithToken(HttpServletResponse response, String jwtToken, String username)
            throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("token", jwtToken)
                .build().toUriString();

        log.info("重導向到前端: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }

    /**
     * 處理認證錯誤，重導向到錯誤頁面。
     */
    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        log.error("處理 OAuth2 登入時發生錯誤", e);

        String errorUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("error", "login_failed")
                .queryParam("message", "登入處理失敗，請稍後再試")
                .build().toUriString();

        response.sendRedirect(errorUrl);
    }

    /**
     * OAuth2 用戶資訊 DTO（內部使用）。
     */
    private record OAuth2UserInfo(String email, String name, String providerId) {
    }
}
