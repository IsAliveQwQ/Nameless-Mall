package com.nameless.mall.user.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.user.api.vo.UserVO;
import com.nameless.mall.user.api.dto.*;
import com.nameless.mall.user.service.UserService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.BeanUtils;

/**
 * 使用者服務的 Controller，負責對外暴露 RESTful API
 */
@Tag(name = "使用者管理", description = "提供使用者註冊、查詢等 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 使用者註冊
     */
    @Operation(summary = "使用者註冊")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SentinelResource(value = "user-register")
    public Result<Boolean> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        boolean isSuccess = userService.register(registerDTO);
        return Result.ok(isSuccess);
    }

    /**
     * 社交登入（內部 API，僅限 auth-service 呼叫）
     */
    @PostMapping("/internal/social-login")
    @SentinelResource(value = "user-social-login")
    public Result<UserAuthDTO> findOrCreateBySocial(@RequestBody SocialUserDTO socialUserDTO) {
        return Result.ok(userService.findOrCreateBySocial(socialUserDTO));
    }

    /**
     * 獲取當前登入使用者的個人資料
     */
    @Operation(summary = "獲取個人資料")
    @GetMapping("/me")
    public Result<UserVO> getProfile(@RequestHeader("X-User-Id") Long userId) {
        UserDTO userDTO = userService.getProfile(userId);
        return Result.ok(toVO(userDTO));
    }

    /**
     * 更新個人資料
     */
    @Operation(summary = "更新個人資料")
    @PutMapping("/me")
    public Result<UserVO> updateProfile(
            @Valid @RequestBody UserUpdateDTO updateDTO,
            @RequestHeader("X-User-Id") Long userId) {
        UserDTO updatedUser = userService.updateProfile(userId, updateDTO);
        return Result.ok(toVO(updatedUser), "個人資料更新成功");
    }

    /**
     * 修改密碼
     */
    @Operation(summary = "修改密碼")
    @PutMapping("/me/password")
    public Result<Void> changePassword(
            @Valid @RequestBody UserPasswordDTO passwordDTO,
            @RequestHeader("X-User-Id") Long userId) {
        userService.changePassword(userId, passwordDTO.getOldPassword(), passwordDTO.getNewPassword());
        return Result.ok(null, "密碼修改成功");
    }

    /**
     * 載入認證資訊（內部 API，僅限 auth-service 呼叫）
     */
    @GetMapping("/internal/loadByUsername/{username}")
    public Result<UserAuthDTO> loadUserByUsername(@PathVariable String username) {
        return Result.ok(userService.loadUserByUsername(username));
    }

    /**
     * 根據 ID 查詢使用者
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getProfile(id);
        return Result.ok(toVO(userDTO));
    }

    private UserVO toVO(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }
}