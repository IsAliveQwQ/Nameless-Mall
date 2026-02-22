package com.nameless.mall.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.promotion.entity.FlashSaleSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 限時特賣商品 Mapper
 */
@Mapper
public interface FlashSaleSkuMapper extends BaseMapper<FlashSaleSku> {

        /**
         * 扣減庫存並增加銷量（原子更新）
         * 
         * @param id       SKU ID
         * @param quantity 數量
         * @return 影響行數
         */
        @Update("UPDATE flash_sale_skus SET flash_sale_stock = flash_sale_stock - #{quantity}, " +
                        "sold_count = sold_count + #{quantity} " +
                        "WHERE id = #{id} AND flash_sale_stock >= #{quantity}")
        int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

        /**
         * 增加庫存並減少銷量 (用於退款/取消)
         */
        @Update("UPDATE flash_sale_skus SET flash_sale_stock = flash_sale_stock + #{quantity}, " +
                        "sold_count = sold_count - #{quantity} " +
                        "WHERE id = #{id}")
        int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
