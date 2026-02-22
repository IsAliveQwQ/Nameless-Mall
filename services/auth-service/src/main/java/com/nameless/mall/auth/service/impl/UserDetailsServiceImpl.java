package com.nameless.mall.auth.service.impl;

import com.nameless.mall.auth.entity.SecurityUser;
import com.nameless.mall.auth.feign.UserServiceFeignClient;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.user.api.dto.UserAuthDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserServiceFeignClient userServiceFeignClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 透過 Feign 呼叫 user-service 取得使用者認證資訊
        Result<UserAuthDTO> result = userServiceFeignClient.loadUserByUsername(username);
        log.info("遠端呼叫使用者資訊, username: {}, success: {}", username, result != null && result.isSuccess());

        // 2. 區分遠端服務不可用與使用者不存在
        if (result == null || result.isFailed()) {
            String failCode = result != null ? result.getCode() : "null";
            // Feign fallback 回傳 SERVICE_UNAVAILABLE 時，不應誤判為「使用者不存在」
            if ("SERVICE_UNAVAILABLE".equals(failCode)) {
                log.error("用戶服務暫時不可用，無法驗證使用者: {}", username);
                throw new org.springframework.security.authentication.AuthenticationServiceException(
                        "用戶服務暫時不可用，請稍後再試");
            }
            log.warn("使用者 {} 不存在或遠端服務回傳資料不完整", username);
            throw new UsernameNotFoundException("使用者 " + username + " 不存在或無法獲取");
        }

        UserAuthDTO userAuthDTO = result.getData();
        if (userAuthDTO == null || userAuthDTO.getId() == null) {
            log.warn("使用者 {} 不存在或遠端服務回傳資料不完整", username);
            throw new UsernameNotFoundException("使用者 " + username + " 不存在或無法獲取");
        }

        // 3. 提取角色列表並轉為權限字串
        List<String> roles = userAuthDTO.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            roles = Collections.emptyList();
        }
        String authoritiesString = String.join(",", roles);

        // 4. 組裝 SecurityUser 物件回傳給 Spring Security
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(userAuthDTO.getId());
        securityUser.setUsername(userAuthDTO.getUsername());
        securityUser.setPassword(userAuthDTO.getPassword());
        securityUser.setStatus(userAuthDTO.getStatus());
        securityUser.setEmail(userAuthDTO.getEmail());
        securityUser.setNickname(userAuthDTO.getNickname());
        securityUser.setAuthorities(AuthorityUtils.commaSeparatedStringToAuthorityList(authoritiesString));

        return securityUser;
    }
}