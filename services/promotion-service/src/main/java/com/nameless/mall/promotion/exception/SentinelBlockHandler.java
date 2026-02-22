package com.nameless.mall.promotion.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 全域阻斷處理器。
 * <p>
 * 用於處理 {@code @SentinelResource} 觸發的限流或熔斷，
 * 統一回傳 {@link ResultCodeEnum#SERVICE_UNAVAILABLE} 語意的 Result，
 * 與全域 {@code ResultCodeEnum} 保持一致。
 * </p>
 */
@Slf4j
public class SentinelBlockHandler {

    /**
     * 處理 getCurrentSession 端點的限流與熔斷請求。
     * <p>
     * 當首頁流量突增超過閾值時觸發，保護後端不被壓垮。
     * </p>
     *
     * @param ex Sentinel 阻斷異常資訊
     * @return 封裝 503 業務碼的 Result，提示使用者稍後重試。不會拋出異常。
     */
    public static Result<com.nameless.mall.promotion.api.vo.FlashSaleSessionVO> handleGetCurrentSessionBlock(
            BlockException ex) {
        log.warn("Sentinel 限流/熔斷觸發 (getCurrentSession): {}", ex.getClass().getSimpleName());
        return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "活動太熱烈了，請稍後再試");
    }

    /**
     * 處理 deductStock 端點的限流與熔斷請求。
     * <p>
     * 避免大量內部訂單請求同時衝擊 Redis 導致穿透。
     * </p>
     *
     * @param deductionList 原請求的庫存扣減清單參數 (需與 controller 簽名一致)
     * @param ex            Sentinel 阻斷異常資訊
     * @return 封裝 503 業務碼的 Result，阻斷後續訂單建立流程
     */
    public static Result<Void> handleDeductStockBlock(
            java.util.List<com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO> deductionList,
            BlockException ex) {
        log.warn("Sentinel 限流/熔斷觸發 (deductStock): {}", ex.getClass().getSimpleName());
        return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "系統繁忙，暫時無法扣減特賣庫存，請稍後再試");
    }

    /**
     * 處理 recoverStock 端點的限流與熔斷請求。
     * <p>
     * 避免因分散式交易補償風暴導致服務無法負載。
     * </p>
     *
     * @param orderSn 原請求的訂單編號
     * @param ex      Sentinel 阻斷異常資訊
     * @return 封裝 503 業務碼的 Result，提示非同步補償任務稍後重試
     */
    public static Result<Void> handleRecoverStockBlock(String orderSn, BlockException ex) {
        log.warn("Sentinel 限流/熔斷觸發 (recoverStock): {}", ex.getClass().getSimpleName());
        return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "系統繁忙，暫時無法返還特賣庫存，請稍後再試");
    }

    /**
     * 處理 syncStock 端點的限流與熔斷請求。
     * <p>
     * 防止後台強制同步操作被惡意或頻繁觸發。
     * </p>
     *
     * @param ex Sentinel 阻斷異常資訊
     * @return 封裝 503 業務碼的 Result
     */
    public static Result<Void> handleSyncStockBlock(BlockException ex) {
        log.warn("Sentinel 限流/熔斷觸發 (syncStock): {}", ex.getClass().getSimpleName());
        return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "系統繁忙，暫時無法同步特賣庫存");
    }
}
