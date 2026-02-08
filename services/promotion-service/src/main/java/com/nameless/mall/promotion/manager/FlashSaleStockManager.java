package com.nameless.mall.promotion.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.entity.FlashSaleLog;
import com.nameless.mall.promotion.entity.FlashSaleSku;
import com.nameless.mall.promotion.entity.FlashSaleUserStat;
import com.nameless.mall.promotion.mapper.FlashSaleLogMapper;
import com.nameless.mall.promotion.mapper.FlashSaleSkuMapper;
import com.nameless.mall.promotion.mapper.FlashSaleUserStatMapper;
import com.nameless.mall.promotion.service.RedisStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 限時特賣庫存扣減管理器。
 * 負責原子扣減庫存、限購檢查、日誌寫入。
 */
@Slf4j
@Component
public class FlashSaleStockManager {

    private final FlashSaleSkuMapper skuMapper;
    private final FlashSaleUserStatMapper userStatMapper;
    private final FlashSaleLogMapper logMapper;
    private final RedisStockService redisStockService;

    public FlashSaleStockManager(FlashSaleSkuMapper skuMapper,
            FlashSaleUserStatMapper userStatMapper,
            FlashSaleLogMapper logMapper,
            RedisStockService redisStockService) {
        this.skuMapper = skuMapper;
        this.userStatMapper = userStatMapper;
        this.logMapper = logMapper;
        this.redisStockService = redisStockService;
    }

    /**
     * 庫存預熱：將 DB 庫存同步至 Redis。
     */
    public void prepare(Long promotionId, Long skuId, Integer stock) {
        redisStockService.prepareStock(promotionId, skuId, stock);
    }

    /**
     * 原子扣減單個 SKU 庫存。
     * 
     * 流程：冪等檢查 → Redis 預扣 → DB 事務（查詢 + 限購 + 扣減 + 日誌）
     * 補償：DB 失敗時回滾 Redis（DB 由 @Transactional 自動回滾）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(FlashSaleDeductionDTO req) {
        // 1. 冪等檢查
        if (isAlreadyDeducted(req)) {
            return;
        }

        // 2. Redis Lua 優先扣減
        deductRedisStockOrThrow(req);

        // 3. DB 事務處理 (含補償)
        try {
            FlashSaleSku sku = findSkuOrThrow(req);
            checkPurchaseLimit(req, sku);
            deductDbStockAndLog(req, sku);
        } catch (Exception e) {
            recoverRedisStock(req);
            throw e;
        }

        log.info("【原子扣減完畢】orderSn={}, qty={}", req.getOrderSn(), req.getQuantity());
    }

    /**
     * 冪等檢查：若該訂單+SKU 已扣減過，則跳過。
     */
    private boolean isAlreadyDeducted(FlashSaleDeductionDTO req) {
        if (logMapper.countByOrderAndSku(req.getOrderSn(), req.getSkuId()) > 0) {
            log.debug("【冪等】扣減已完成，跳過: orderSn={}", req.getOrderSn());
            return true;
        }
        return false;
    }

    /**
     * Redis Lua 原子扣減，失敗則拋出異常。
     */
    private void deductRedisStockOrThrow(FlashSaleDeductionDTO req) {
        if (!redisStockService.deduct(req.getPromotionId(), req.getSkuId(), req.getQuantity())) {
            throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT, "特賣商品已售罄 (Redis)");
        }
    }

    /**
     * 查詢特賣 SKU，不存在則拋出異常。
     */
    private FlashSaleSku findSkuOrThrow(FlashSaleDeductionDTO req) {
        FlashSaleSku sku = skuMapper.selectOne(new LambdaQueryWrapper<FlashSaleSku>()
                .eq(FlashSaleSku::getPromotionId, req.getPromotionId())
                .eq(FlashSaleSku::getVariantId, req.getSkuId()));

        if (sku == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND, "特賣商品不存在");
        }
        return sku;
    }

    /**
     * DB 原子扣減庫存並記錄日誌。
     */
    private void deductDbStockAndLog(FlashSaleDeductionDTO req, FlashSaleSku sku) {
        // DB 原子更新
        if (skuMapper.decreaseStock(sku.getId(), req.getQuantity()) == 0) {
            throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT, "系統繁忙，請稍後再試");
        }

        // 記錄扣減日誌
        FlashSaleLog logEntry = FlashSaleLog.builder()
                .promotionId(req.getPromotionId())
                .skuId(req.getSkuId())
                .userId(req.getUserId())
                .orderSn(req.getOrderSn())
                .quantity(req.getQuantity())
                .deductedAt(LocalDateTime.now())
                .build();
        logMapper.insert(logEntry);
    }

    /**
     * 補償：回滾 Redis 庫存（用於 DB 事務失敗時）。
     */
    private void recoverRedisStock(FlashSaleDeductionDTO req) {
        log.warn("【高併發補償】交易失敗，退回 Redis 庫存: promoId={}, skuId={}",
                req.getPromotionId(), req.getSkuId());
        redisStockService.recoverStock(req.getPromotionId(), req.getSkuId(), req.getQuantity());
    }

    /**
     * 僅 rollback Redis 庫存，不涉及 DB。
     * 用於批量扣減過程中，局部 SKU 失敗時的全量補償。
     */
    public void rollbackRedisOnly(FlashSaleDeductionDTO req) {
        log.warn("【補償】僅 rollback Redis 庫存: promoId={}, skuId={}, qty={}",
                req.getPromotionId(), req.getSkuId(), req.getQuantity());
        redisStockService.recoverStock(req.getPromotionId(), req.getSkuId(), req.getQuantity());
    }

    /**
     * 原子退回 SKU 庫存 (包含刪除 Log)。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recover(FlashSaleDeductionDTO req) {
        FlashSaleLog logEntry = logMapper.selectOne(new LambdaQueryWrapper<FlashSaleLog>()
                .eq(FlashSaleLog::getOrderSn, req.getOrderSn())
                .eq(FlashSaleLog::getSkuId, req.getSkuId()));

        if (logEntry == null) {
            return;
        }

        // 呼叫內部核心退還邏輯
        recoverStockOnly(req, logEntry.getQuantity());

        // 刪除 Log
        logMapper.deleteById(logEntry.getId());
        log.info("【特賣退回成功】orderSn={} (含日誌清理)", req.getOrderSn());
    }

    /**
     * 核心庫存退還邏輯 (不含 Log 操作)。
     * 專供 FlashSalePromotionServiceImpl 在搶佔 Log 刪除權限後調用。
     * 
     * @param req      包含 SkuId, PromotionId, UserId 的請求對象
     * @param quantity 要退還的數量 (因為 DTO 可能來自 Log，這裡明確傳入)
     */
    @Transactional(rollbackFor = Exception.class)
    public void recoverStockOnly(FlashSaleDeductionDTO req, Integer quantity) {
        // 1. 查詢對應的特賣 SKU
        FlashSaleSku sku = skuMapper.selectOne(new LambdaQueryWrapper<FlashSaleSku>()
                .eq(FlashSaleSku::getPromotionId, req.getPromotionId())
                .eq(FlashSaleSku::getVariantId, req.getSkuId()));

        // 2. 退還 DB 庫存並同步回補 Redis
        if (sku != null) {
            skuMapper.increaseStock(sku.getId(), quantity);
            redisStockService.recoverStock(req.getPromotionId(), req.getSkuId(), quantity);
        }

        // 3. 更新用戶購買統計（扣減已購數量）
        FlashSaleUserStat stat = userStatMapper.selectForUpdate(
                req.getPromotionId(), req.getSkuId(), req.getUserId());
        if (stat != null) {
            int newCount = Math.max(0, stat.getPurchasedCount() - quantity);
            stat.setPurchasedCount(newCount);
            stat.setUpdatedAt(LocalDateTime.now());
            userStatMapper.updateById(stat);
        }
    }

    /**
     * 限購檢查與用戶統計更新。
     */
    private void checkPurchaseLimit(FlashSaleDeductionDTO req, FlashSaleSku sku) {
        // 1. 若未設定限購，直接跳過
        if (sku.getLimitPerUser() == null || sku.getLimitPerUser() <= 0) {
            return;
        }

        // 2. 行鎖查詢用戶已購數量（SELECT FOR UPDATE）
        FlashSaleUserStat stat = userStatMapper.selectForUpdate(
                req.getPromotionId(), req.getSkuId(), req.getUserId());
        int current = (stat != null) ? stat.getPurchasedCount() : 0;

        // 3. 校驗是否超過每人限購上限
        if (current + req.getQuantity() > sku.getLimitPerUser()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN,
                    "超過每人限購數量 (" + sku.getLimitPerUser() + " 件)");
        }

        // 4. 更新或新增用戶購買統計
        if (stat == null) {
            FlashSaleUserStat newStat = FlashSaleUserStat.builder()
                    .promotionId(req.getPromotionId())
                    .skuId(req.getSkuId())
                    .userId(req.getUserId())
                    .purchasedCount(req.getQuantity())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userStatMapper.insert(newStat);
        } else {
            stat.setPurchasedCount(current + req.getQuantity());
            stat.setUpdatedAt(LocalDateTime.now());
            userStatMapper.updateById(stat);
        }
    }
}
