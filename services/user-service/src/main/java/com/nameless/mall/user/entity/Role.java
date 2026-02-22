package com.nameless.mall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 角色實體 */
@Data
@TableName("roles")
public class Role {
    private Long id;
    private String name;
    private String description;
}