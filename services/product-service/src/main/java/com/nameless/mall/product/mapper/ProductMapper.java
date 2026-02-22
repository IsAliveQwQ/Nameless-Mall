package com.nameless.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品主表 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
