package com.nameless.mall.order.service;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.order.api.dto.FlashSaleSubmitDTO;

/**
 * 特賣異步下單服務介面
 */
public interface FlashSaleOrderService {

    /**
     * 執行特賣活動請購 (異步)
     *
     * @param userId 當前用戶 ID
     * @param dto    提交參數 (SKU, PromotionId)
     * @return OrderToken 排隊權杖
     */
    String submitFlashSale(Long userId, FlashSaleSubmitDTO dto);

    /**
     * 查詢訂單處理結果
     *
     * @param orderToken 排隊權杖
     * @return 處理狀態 (PENDING / SUCCESS / FAILED)
     */
    Result<Object> checkOrderResult(String orderToken);
}
