package com.nameless.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.user.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用戶收貨地址 Mapper
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
