package com.nameless.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.user.api.dto.UserAuthDTO;
import com.nameless.mall.user.api.dto.UserRegisterDTO;
import com.nameless.mall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    boolean register(UserRegisterDTO registerDTO);

    UserAuthDTO loadUserByUsername(String username);
}