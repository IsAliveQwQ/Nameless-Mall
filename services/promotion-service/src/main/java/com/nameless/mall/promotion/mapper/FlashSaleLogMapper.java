package com.nameless.mall.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.promotion.entity.FlashSaleLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 特賣扣減日誌 Mapper */
@Mapper
public interface FlashSaleLogMapper extends BaseMapper<FlashSaleLog> {

    /**
     * 檢查該訂單是否已扣減過該商品 (冪等性)
     */
    @Select("SELECT COUNT(*) FROM flash_sale_logs WHERE order_sn = #{orderSn} AND sku_id = #{skuId}")
    int countByOrderAndSku(@Param("orderSn") String orderSn, @Param("skuId") Long skuId);

    /**
     * 統計用戶在該活動中已購買的數量 (限購檢查)
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM flash_sale_logs WHERE user_id = #{userId} AND promotion_id = #{promotionId}")
    int sumQuantityByUser(@Param("userId") Long userId, @Param("promotionId") Long promotionId);
}
