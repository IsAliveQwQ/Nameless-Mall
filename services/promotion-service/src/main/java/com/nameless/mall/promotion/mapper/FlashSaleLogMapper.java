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

}
