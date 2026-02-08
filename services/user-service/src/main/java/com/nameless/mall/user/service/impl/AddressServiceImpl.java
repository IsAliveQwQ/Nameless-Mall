package com.nameless.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.user.api.dto.AddressCreateDTO;
import com.nameless.mall.user.api.dto.AddressDTO;
import com.nameless.mall.user.entity.UserAddress;
import com.nameless.mall.user.mapper.UserAddressMapper;
import com.nameless.mall.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用戶收貨地址服務實作
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper addressMapper;

    @Override
    public List<AddressDTO> listByUserId(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdatedAt);

        return addressMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AddressDTO getById(Long addressId, Long userId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ADDRESS_NOT_FOUND, "地址不存在或無權限存取");
        }
        return toDTO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressDTO create(Long userId, AddressCreateDTO dto) {
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(dto, address);
        address.setUserId(userId);
        address.setIsDefault(0);

        // 如果要設為預設地址，先清除其他預設
        if (Boolean.TRUE.equals(dto.getSetDefault())) {
            clearDefaultAddress(userId);
            address.setIsDefault(1);
        }

        // 如果是第一個地址，自動設為預設
        Long count = addressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        if (count == 0) {
            address.setIsDefault(1);
        }

        addressMapper.insert(address);
        return toDTO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressDTO update(Long addressId, Long userId, AddressCreateDTO dto) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ADDRESS_NOT_FOUND, "地址不存在或無權限存取");
        }

        BeanUtils.copyProperties(dto, address);

        // 如果要設為預設地址
        if (Boolean.TRUE.equals(dto.getSetDefault())) {
            clearDefaultAddress(userId);
            address.setIsDefault(1);
        }

        addressMapper.updateById(address);
        return toDTO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long addressId, Long userId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ADDRESS_NOT_FOUND, "地址不存在或無權限存取");
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId, Long userId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ADDRESS_NOT_FOUND, "地址不存在或無權限存取");
        }

        // 先清除其他預設
        clearDefaultAddress(userId);

        // 設定新的預設
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    @Override
    public AddressDTO getDefaultAddress(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1);

        UserAddress address = addressMapper.selectOne(wrapper);
        return address != null ? toDTO(address) : null;
    }

    /**
     * 清除用戶的預設地址標記
     */
    private void clearDefaultAddress(Long userId) {
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0);
        addressMapper.update(null, wrapper);
    }

    /**
     * Entity 轉 DTO
     */
    private AddressDTO toDTO(UserAddress entity) {
        AddressDTO dto = new AddressDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
