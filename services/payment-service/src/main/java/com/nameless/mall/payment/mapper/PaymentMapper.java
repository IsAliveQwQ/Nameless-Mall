package com.nameless.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付單 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

}
