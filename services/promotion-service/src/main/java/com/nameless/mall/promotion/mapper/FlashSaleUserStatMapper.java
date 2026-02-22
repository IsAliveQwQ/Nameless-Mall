package com.nameless.mall.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.promotion.entity.FlashSaleUserStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 特賣用戶限購統計 Mapper */
@Mapper
public interface FlashSaleUserStatMapper extends BaseMapper<FlashSaleUserStat> {

        /**
         * 悲觀鎖查詢（行鎖）
         * 用於確保同一用戶的併發請求串行化
         * 注意：Seata AT 模式下，SELECT FOR UPDATE 會生成全局鎖。
         */
        @Select("SELECT * FROM flash_sale_user_stats " +
                        "WHERE promotion_id = #{promotionId} AND sku_id = #{skuId} AND user_id = #{userId} " +
                        "FOR UPDATE")
        FlashSaleUserStat selectForUpdate(@Param("promotionId") Long promotionId,
                        @Param("skuId") Long skuId,
                        @Param("userId") Long userId);
}
