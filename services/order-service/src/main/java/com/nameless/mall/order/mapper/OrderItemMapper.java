package com.nameless.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 訂單項目 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
