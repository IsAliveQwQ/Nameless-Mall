package com.nameless.mall.user.service;

import com.nameless.mall.user.api.dto.AddressCreateDTO;
import com.nameless.mall.user.api.dto.AddressDTO;
import java.util.List;

/**
 * 用戶收貨地址服務介面
 */
public interface AddressService {

    /**
     * 獲取用戶的所有收貨地址
     * @param userId 用戶 ID
     * @return 地址列表
     */
    List<AddressDTO> listByUserId(Long userId);

    /**
     * 根據 ID 獲取地址詳情
     * @param addressId 地址 ID
     * @param userId 用戶 ID (用於權限驗證)
     * @return 地址詳情
     */
    AddressDTO getById(Long addressId, Long userId);

    /**
     * 新增收貨地址
     * @param userId 用戶 ID
     * @param dto 地址資訊
     * @return 新增的地址
     */
    AddressDTO create(Long userId, AddressCreateDTO dto);

    /**
     * 更新收貨地址
     * @param addressId 地址 ID
     * @param userId 用戶 ID (用於權限驗證)
     * @param dto 更新資訊
     * @return 更新後的地址
     */
    AddressDTO update(Long addressId, Long userId, AddressCreateDTO dto);

    /**
     * 刪除收貨地址
     * @param addressId 地址 ID
     * @param userId 用戶 ID (用於權限驗證)
     */
    void delete(Long addressId, Long userId);

    /**
     * 設定預設地址
     * @param addressId 地址 ID
     * @param userId 用戶 ID
     */
    void setDefault(Long addressId, Long userId);

    /**
     * 獲取用戶的預設地址
     * @param userId 用戶 ID
     * @return 預設地址，若無則返回 null
     */
    AddressDTO getDefaultAddress(Long userId);
}
