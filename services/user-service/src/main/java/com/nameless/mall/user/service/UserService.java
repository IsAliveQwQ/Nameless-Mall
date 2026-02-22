package com.nameless.mall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.user.api.dto.*;

import com.nameless.mall.user.entity.User;

public interface UserService extends IService<User> {

    /**
     * 註冊新使用者
     * 
     * @param registerDTO 包含使用者名稱、密碼等資訊的 DTO
     * @return 是否註冊成功
     */
    boolean register(UserRegisterDTO registerDTO);

    /**
     * 根據使用者名稱，載入認證所需的使用者資訊(包含角色)
     * 這是專門給 auth-service 遠端呼叫使用的方法。
     * 
     * @param username 使用者名稱
     * @return 包含密碼和角色的使用者 DTO
     */
    UserAuthDTO loadUserByUsername(String username);

    /**
     * 根據社交登入的資訊，尋找或建立使用者
     * 
     * @param socialUserDTO 包含來自第三方（如Google）的使用者資訊
     * @return 系統內對應的使用者認證 DTO
     */
    UserAuthDTO findOrCreateBySocial(SocialUserDTO socialUserDTO);

    /**
     * 更新當前登入者的個人資料
     * 
     * @param userId    當前登入者的使用者 ID (從 JWT Token 中解析)
     * @param updateDTO 包含要更新資料的 DTO
     * @return 更新後的使用者資訊 DTO
     */
    UserDTO updateProfile(Long userId, UserUpdateDTO updateDTO);

    /**
     * 獲取當前登入者的個人資料
     * 
     * @param userId 當前登入者的使用者 ID (從 JWT Token 中解析)
     * @return 使用者資訊 DTO（不含敏感資訊）
     */
    UserDTO getProfile(Long userId);

    /**
     * 修改密碼
     * 
     * @param userId      當前登入者的使用者 ID (從 JWT Token 中解析)
     * @param oldPassword 舊密碼
     * @param newPassword 新密碼
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}