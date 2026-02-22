package com.nameless.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.user.api.dto.*;
import com.nameless.mall.user.entity.Role;
import com.nameless.mall.user.entity.User;
import com.nameless.mall.user.entity.UserRole;

import com.nameless.mall.user.mapper.RoleMapper;
import com.nameless.mall.user.mapper.UserMapper;
import com.nameless.mall.user.mapper.UserRoleMapper;
import com.nameless.mall.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 使用者服務的業務邏輯實現類
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 使用者狀態：正常啟用 (0:正常, 1:停用) */
    private static final int STATUS_ACTIVE = 0;

    /** 用戶事件 MQ Exchange */
    private static final String USER_EXCHANGE = "mall.user.exchange";

    /** 用戶註冊事件 Routing Key */
    private static final String USER_REGISTERED_KEY = "user.registered";

    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public boolean register(UserRegisterDTO registerDTO) {
        // 1. 檢查使用者名稱是否重複
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername());
        if (this.baseMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCodeEnum.USERNAME_ALREADY_EXISTS);
        }

        // 2. 建立使用者實體並加密密碼
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail() != null ? registerDTO.getEmail() : "");
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(STATUS_ACTIVE);

        // 3. 儲存使用者
        boolean result = this.save(user);
        // 4. 發送使用者註冊 MQ 事件
        if (result) {
            publishUserRegisteredEvent(user.getId());
        }
        return result;
    }

    @Override
    public UserAuthDTO loadUserByUsername(String username) {
        // 1. 依使用者名稱查詢使用者
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }

        // 2. 查詢使用者對應的角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));

        if (CollectionUtils.isEmpty(userRoles)) {
            // 若查無明確角色關聯，則預設給予 USER 權限
            return buildUserAuthDTO(user, Collections.singletonList("USER"));
        }

        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());

        // 3. 組裝認證 DTO 回傳
        return buildUserAuthDTO(user, roleNames);
    }

    @Override
    public UserAuthDTO findOrCreateBySocial(SocialUserDTO socialUserDTO) {
        // 1. 以 Email 查詢是否已有使用者
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, socialUserDTO.getEmail()));

        if (user == null) {
            // 2. 使用者不存在，建立新帳號
            user = new User();
            // 3. 防止使用者名稱衝突，重複時加上隨機後綴
            String baseUsername = socialUserDTO.getUsername();
            if (this.baseMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, baseUsername)) > 0) {
                user.setUsername(baseUsername + "_" + UUID.randomUUID().toString().substring(0, 4));
            } else {
                user.setUsername(baseUsername);
            }
            user.setEmail(socialUserDTO.getEmail());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setStatus(STATUS_ACTIVE);
            // 4. 儲存新使用者並發送註冊事件
            this.save(user);
            publishUserRegisteredEvent(user.getId());
        }

        // 5. 查詢使用者角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));

        List<String> roleNames;
        if (!CollectionUtils.isEmpty(userRoles)) {
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());
        } else {
            // 第三方登入新帳號，預設賦予 USER 權限
            roleNames = Collections.singletonList("USER");
        }

        // 6. 組裝認證 DTO 回傳
        return buildUserAuthDTO(user, roleNames);
    }

    @Override
    public UserDTO updateProfile(Long userId, UserUpdateDTO updateDTO) {
        User user = findUserOrThrow(userId);

        boolean basicModified = updateBasicInfo(user, updateDTO);
        boolean passwordModified = handlePasswordChangeIfRequested(user, updateDTO);

        if (basicModified || passwordModified) {
            this.updateById(user);
        }

        return buildUserDTO(user);
    }

    /**
     * 根據用戶 ID 查詢用戶，若不存在則拋出異常。
     *
     * @param userId 用戶 ID
     * @return 用戶實體
     * @throws BusinessException 若用戶不存在
     */
    private User findUserOrThrow(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 更新用戶基本資料（Email, Phone, Nickname, Avatar）。
     *
     * @param user      用戶實體
     * @param updateDTO 更新資料
     * @return 是否有實際修改
     */
    private boolean updateBasicInfo(User user, UserUpdateDTO updateDTO) {
        boolean isModified = false;

        // 處理 Email 更新
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()
                && !updateDTO.getEmail().equals(user.getEmail())) {
            user.setEmail(updateDTO.getEmail());
            isModified = true;
        }

        // 處理 Phone 更新
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
            isModified = true;
        }

        // 處理 Nickname 更新
        if (updateDTO.getNickname() != null) {
            user.setNickname(updateDTO.getNickname());
            isModified = true;
        }

        // 處理 Avatar 更新
        if (updateDTO.getAvatar() != null) {
            user.setAvatar(updateDTO.getAvatar());
            isModified = true;
        }

        return isModified;
    }

    /**
     * 處理密碼更新請求。
     *
     * 若 updateDTO 中包含新密碼，則驗證舊密碼並更新。
     *
     * @param user      用戶實體
     * @param updateDTO 更新資料
     * @return 是否有實際修改密碼
     * @throws BusinessException 若舊密碼不正確或新密碼長度不符
     */
    private boolean handlePasswordChangeIfRequested(User user, UserUpdateDTO updateDTO) {
        if (updateDTO.getNewPassword() == null || updateDTO.getNewPassword().isEmpty()) {
            return false;
        }

        if (updateDTO.getCurrentPassword() == null ||
                !passwordEncoder.matches(updateDTO.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_INCORRECT, "目前密碼不正確");
        }

        if (updateDTO.getNewPassword().length() < 6 || updateDTO.getNewPassword().length() > 20) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_TOO_WEAK, "新密碼長度必須在 6 到 20 個字元之間");
        }

        user.setPassword(passwordEncoder.encode(updateDTO.getNewPassword()));
        return true;
    }

    @Override
    public UserDTO getProfile(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        return buildUserDTO(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_INCORRECT, "舊密碼不正確");
        }

        // 密碼長度驗證（與 handlePasswordChangeIfRequested 邏輯一致）
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_TOO_WEAK, "新密碼長度必須在 6 到 20 個字元之間");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    private UserAuthDTO buildUserAuthDTO(User user, List<String> roleNames) {
        UserAuthDTO dto = new UserAuthDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setStatus(user.getStatus());
        dto.setRoles(roleNames);
        return dto;
    }

    private UserDTO buildUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    /**
     * 發送用戶註冊事件至 MQ。
     * 
     * 用於通知下游服務（如 coupon-service）進行新用戶優惠券發放等操作。
     * 若發送失敗，僅記錄錯誤，不阻斷主流程。
     * 
     * @param userId 用戶 ID
     */
    private void publishUserRegisteredEvent(Long userId) {
        try {
            rabbitTemplate.convertAndSend(USER_EXCHANGE, USER_REGISTERED_KEY, userId);
            log.info("發送用戶註冊事件成功: userId={}", userId);
        } catch (Exception e) {
            log.error("發送用戶註冊事件失敗: userId={}", userId, e);
        }
    }

}