package com.nameless.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.product.entity.Variant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品規格庫存 Mapper
 */
@Mapper
public interface VariantMapper extends BaseMapper<Variant> {

    /**
     * 原子操作：扣減庫存
     * 只有當 stock >= quantity 時才會扣減，避免超賣
     * 
     * @param variantId 規格 ID
     * @param quantity  扣減數量
     * @return 受影響的行數（0 表示庫存不足）
     */
    @Update("UPDATE variants SET stock = stock - #{quantity} WHERE id = #{variantId} AND stock >= #{quantity}")
    int decreaseStock(@Param("variantId") Long variantId, @Param("quantity") Integer quantity);

    /**
     * 原子操作：返還庫存
     * 
     * @param variantId 規格 ID
     * @param quantity  返還數量
     * @return 受影響的行數
     */
    @Update("UPDATE variants SET stock = stock + #{quantity} WHERE id = #{variantId}")
    int increaseStock(@Param("variantId") Long variantId, @Param("quantity") Integer quantity);
}
