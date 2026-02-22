package com.nameless.mall.auth.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.user.api.dto.SocialUserDTO;
import com.nameless.mall.user.api.dto.UserAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", fallbackFactory = UserServiceFeignFallback.class)
public interface UserServiceFeignClient {

    @GetMapping("/users/internal/loadByUsername/{username}")
    Result<UserAuthDTO> loadUserByUsername(@PathVariable("username") String username);

    @PostMapping("/users/internal/social-login")
    Result<UserAuthDTO> findOrCreateBySocial(@RequestBody SocialUserDTO socialUserDTO);
}