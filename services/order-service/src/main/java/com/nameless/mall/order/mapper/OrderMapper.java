package com.nameless.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 訂單主表 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 插入特賣活動購買記錄 (用於 DB 冪等性控制)
     * 利用 UNIQUE KEY (user_id, promotion_id, sku_id) 實現每人限購一個
     */
    @org.apache.ibatis.annotations.Insert("INSERT INTO oms_flash_sale_record (user_id, promotion_id, sku_id, order_sn, quantity) VALUES (#{userId}, #{promotionId}, #{skuId}, #{orderSn}, #{quantity})")
    int insertFlashSaleRecord(@org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("promotionId") Long promotionId,
            @org.apache.ibatis.annotations.Param("skuId") Long skuId,
            @org.apache.ibatis.annotations.Param("orderSn") String orderSn,
            @org.apache.ibatis.annotations.Param("quantity") Integer quantity);

    /**
     * 查詢指定訂單中屬於特賣的 SKU ID 列表。
     * 用於取消訂單時過濾特賣品，避免誤退一般庫存。
     */
    @org.apache.ibatis.annotations.Select("SELECT sku_id FROM oms_flash_sale_record WHERE order_sn = #{orderSn}")
    java.util.List<Long> selectFlashSaleSkuIds(@org.apache.ibatis.annotations.Param("orderSn") String orderSn);
}
