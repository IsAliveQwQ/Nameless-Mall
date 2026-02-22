package com.nameless.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.product.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分類 Mapper
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
