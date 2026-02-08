package com.nameless.mall.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 優惠券範本 Mapper
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /**
     * 原子操作：扣減優惠券剩餘數量
     * 只有當 remain_count > 0 時才會扣減，避免超發
     * 
     * @param templateId 優惠券範本 ID
     * @return 受影響的行數（0 表示扣減失敗）
     */
    @Update("UPDATE coupon_templates SET remain_count = remain_count - 1 WHERE id = #{templateId} AND remain_count > 0")
    int decreaseRemainCount(@Param("templateId") Long templateId);
}
