package com.nameless.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.product.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/** 商品標籤 Mapper */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
