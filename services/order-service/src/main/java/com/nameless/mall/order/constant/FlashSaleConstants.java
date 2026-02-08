package com.nameless.mall.order.constant;

/** 特賣活動 Redis Key 與狀態常量。 */
public class FlashSaleConstants {
    private FlashSaleConstants() {
    }

    // Redis 鍵前綴
    public static final String CACHE_STOCK_PREFIX = "flash_sale:stock:"; // + promotionId:skuId
    public static final String CACHE_ORDER_PREFIX = "flash_sale:order:"; // + userId:skuId

    // Redis 訂單狀態
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";
}
