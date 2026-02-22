package com.nameless.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.product.entity.ProductTag;
import org.apache.ibatis.annotations.Mapper;

/** 商品-標籤關聯 Mapper */
@Mapper
public interface ProductTagMapper extends BaseMapper<ProductTag> {
}
