package com.nameless.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.order.entity.OrderShipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 訂單運送資訊 Mapper
 */
@Mapper
public interface OrderShipmentMapper extends BaseMapper<OrderShipment> {
}
