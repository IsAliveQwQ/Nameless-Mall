package com.nameless.mall.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.promotion.api.dto.FlashSaleDeductionDTO;
import com.nameless.mall.promotion.api.dto.FlashSalePromotionDTO;
import com.nameless.mall.promotion.api.vo.FlashSaleProductVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.client.ProductClient;
import com.nameless.mall.promotion.entity.FlashSaleLog;
import com.nameless.mall.promotion.entity.FlashSalePromotion;
import com.nameless.mall.promotion.entity.FlashSaleSku;
import com.nameless.mall.promotion.manager.FlashSaleStockManager;
import com.nameless.mall.promotion.manager.FlashSaleSyncManager;
import com.nameless.mall.promotion.enums.PromotionStatus;
import com.nameless.mall.promotion.mapper.FlashSaleLogMapper;
import com.nameless.mall.promotion.mapper.FlashSalePromotionMapper;
import com.nameless.mall.promotion.mapper.FlashSaleSkuMapper;
import com.nameless.mall.promotion.service.FlashSalePromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 特賣活動服務實作。
 * 負責協調特賣活動的生命週期、庫存扣減與數據同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSalePromotionServiceImpl extends ServiceImpl<FlashSalePromotionMapper, FlashSalePromotion>
        implements FlashSalePromotionService {

    private final FlashSaleSkuMapper flashSaleSkuMapper;
    private final FlashSaleLogMapper flashSaleLogMapper;
    private final ProductClient productClient;
    private final FlashSaleStockManager stockManager;
    private final FlashSaleSyncManager syncManager;

    /**
     * 獲取當前進行中的活動列表。
     */
    @Override
    public List<FlashSalePromotionDTO> getCurrentPromotions() {
        LocalDateTime now = LocalDateTime.now();
        return this.list(new LambdaQueryWrapper<FlashSalePromotion>()
                .eq(FlashSalePromotion::getStatus, PromotionStatus.ACTIVE.getCode())
                .le(FlashSalePromotion::getStartTime, now)
                .ge(FlashSalePromotion::getEndTime, now))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FlashSalePromotionDTO getPromotionById(Long id) {
        return Optional.ofNullable(this.getById(id)).map(this::toDTO).orElse(null);
    }

    /**
     * 獲取當前或即將開始的特賣場次資訊。
     */
    @Override
    public FlashSaleSessionVO getCurrentSession() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 查詢進行中或即將開始的活動
        FlashSalePromotion promotion = findActiveOrUpcomingPromotion(now);
        if (promotion == null) {
            return null;
        }

        // 2. 取得該活動下所有特賣 SKU
        List<FlashSaleSku> skus = flashSaleSkuMapper.selectList(
                new LambdaQueryWrapper<FlashSaleSku>().eq(FlashSaleSku::getPromotionId, promotion.getId()));

        // 3. 透過 Feign 批量填充商品資訊
        List<FlashSaleProductVO> products = skus.isEmpty()
                ? Collections.emptyList()
                : enrichWithProductInfo(skus);

        // 4. 組裝場次 VO（含狀態與倒數計時）
        return buildSessionVO(promotion, products, now);
    }

    /**
     * 執行庫存原子扣減。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(List<FlashSaleDeductionDTO> deductionList) {
        if (CollectionUtils.isEmpty(deductionList)) {
            return;
        }

        // 排序以防止資料庫死鎖
        deductionList.sort(Comparator.comparing(FlashSaleDeductionDTO::getSkuId));

        // 記錄本次批量中已成功扣減 Redis 的 SKU，用於失敗時的全量補償
        List<FlashSaleDeductionDTO> successList = new ArrayList<>();

        try {
            for (FlashSaleDeductionDTO req : deductionList) {
                // 執行扣減 (內部包含 Redis 與 DB 操作)
                stockManager.deduct(req);
                successList.add(req);
            }
        } catch (Exception e) {
            log.error("【特賣服務】批量扣減失敗，啟動 Redis 全量補償機制: {}", e.getMessage());
            // 只要有一個失敗，必須退回本次已扣減的所有 Redis 庫存
            successList.forEach(stockManager::rollbackRedisOnly);
            throw e; // 拋出例外以觸發 DB 回滾
        }
    }

    /**
     * 退回特賣庫存。
     * 先執行原子刪除，確保並行重複消費時的冪等性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverStock(String orderSn) {
        // 1. 根據訂單號查詢所有特賣扣減紀錄
        List<FlashSaleLog> logs = flashSaleLogMapper.selectList(
                new LambdaQueryWrapper<FlashSaleLog>().eq(FlashSaleLog::getOrderSn, orderSn));

        if (CollectionUtils.isEmpty(logs)) {
            return;
        }

        // 2. 逐筆處理每條扣減紀錄
        logs.forEach(logEntry -> {
            // 3. 原子刪除檢查 (Atomic Check-and-Act)
            // 只有成功刪除這條記錄的線程，才有資格執行後續的退庫存操作
            int deleted = flashSaleLogMapper.deleteById(logEntry.getId());

            if (deleted > 0) {
                log.info("【特賣補償】成功搶佔補償權限，執行庫存退還: sn={}, sku={}, qty={}",
                        orderSn, logEntry.getSkuId(), logEntry.getQuantity());

                // 4. 構建退還請求並執行 DB + Redis 庫存回補
                FlashSaleDeductionDTO dto = new FlashSaleDeductionDTO();
                BeanUtils.copyProperties(logEntry, dto);
                // 呼叫只負責退庫存的方法，避免重複刪除 Log
                stockManager.recoverStockOnly(dto, logEntry.getQuantity());
            } else {
                log.info("【特賣補償】並行消費偵測 - 該記錄已被其他線程處理，跳過: id={}", logEntry.getId());
            }
        });
    }

    /**
     * 手動觸發快取預熱與庫存同步。
     */
    @Override
    public void syncPromotionStock() {
        LocalDateTime now = LocalDateTime.now();
        this.list(new LambdaQueryWrapper<FlashSalePromotion>()
                .eq(FlashSalePromotion::getStatus, PromotionStatus.ACTIVE.getCode())
                .ge(FlashSalePromotion::getEndTime, now))
                .forEach(p -> {
                    try {
                        // 1. 同步商品資料 (MySQL)
                        syncManager.syncPromotion(p);

                        // 2. 庫存預熱：將 DB 最新庫存同步寫入 Redis
                        List<FlashSaleSku> skus = flashSaleSkuMapper.selectList(
                                new LambdaQueryWrapper<FlashSaleSku>().eq(FlashSaleSku::getPromotionId, p.getId()));

                        if (!CollectionUtils.isEmpty(skus)) {
                            skus.forEach(sku -> stockManager.prepare(
                                    sku.getPromotionId(),
                                    sku.getVariantId(),
                                    sku.getFlashSaleStock())); // 使用當前 DB 剩餘庫存
                            log.info("【庫存預熱】活動 ID={}, SKU 數量={} (已同步至 Redis)", p.getId(), skus.size());
                        }

                    } catch (Exception e) {
                        log.error("同步活動失敗: id={}", p.getId(), e);
                    }
                });
    }

    private FlashSalePromotion findActiveOrUpcomingPromotion(LocalDateTime now) {
        // 1. 優先查詢當前時間範圍內的進行中活動
        FlashSalePromotion p = this.getOne(new LambdaQueryWrapper<FlashSalePromotion>()
                .eq(FlashSalePromotion::getStatus, PromotionStatus.ACTIVE.getCode())
                .le(FlashSalePromotion::getStartTime, now)
                .ge(FlashSalePromotion::getEndTime, now)
                .orderByDesc(FlashSalePromotion::getStartTime)
                .last("LIMIT 1"));

        if (p != null) {
            return p;
        }

        // 2. 若無進行中活動，查詢最近即將開始的活動
        return this.getOne(new LambdaQueryWrapper<FlashSalePromotion>()
                .eq(FlashSalePromotion::getStatus, PromotionStatus.ACTIVE.getCode())
                .gt(FlashSalePromotion::getStartTime, now)
                .orderByAsc(FlashSalePromotion::getStartTime)
                .last("LIMIT 1"));
    }

    private List<FlashSaleProductVO> enrichWithProductInfo(List<FlashSaleSku> skus) {
        // 1. 提取所有規格 ID
        List<Long> variantIds = skus.stream()
                .map(FlashSaleSku::getVariantId)
                .collect(Collectors.toList());

        Map<Long, VariantDTO> variantMap = Collections.emptyMap();

        try {
            // 2. 批量呼叫 Product Service 取得規格詳情
            Result<List<VariantDTO>> result = productClient.getVariantsBatch(variantIds);
            if (result != null && result.getData() != null) {
                variantMap = result.getData().stream()
                        .collect(Collectors.toMap(VariantDTO::getId, Function.identity(), (v1, v2) -> v1));
            }
        } catch (Exception e) {
            log.error("透過 Feign 獲取商品詳情失敗", e);
        }

        // 3. 合併特賣 SKU 與商品資訊，組裝產品 VO
        Map<Long, VariantDTO> finalMap = variantMap;
        return skus.stream()
                .map(sku -> buildProductVO(sku, finalMap.get(sku.getVariantId())))
                .collect(Collectors.toList());
    }

    private FlashSaleProductVO buildProductVO(FlashSaleSku sku, VariantDTO variant) {
        BigDecimal original = calculateOriginalPrice(sku, variant);
        String discountLabel = calculateDiscountLabel(sku.getFlashSalePrice(), original);

        String name = (variant != null)
                ? variant.getProductName() + " " + variant.getSku()
                : "未知商品";

        String imageUrl = (variant != null) ? variant.getImage() : null;
        String stockStatus = (sku.getFlashSaleStock() > 0) ? "有貨" : "售罄";

        return FlashSaleProductVO.builder()
                .id(sku.getId())
                .productId(sku.getProductId())
                .variantId(sku.getVariantId())
                .name(name)
                .imageUrl(imageUrl)
                .originalPrice(original)
                .flashPrice(sku.getFlashSalePrice())
                .discountLabel(discountLabel)
                .stockStatus(stockStatus)
                .build();
    }

    private BigDecimal calculateOriginalPrice(FlashSaleSku sku, VariantDTO v) {
        if (v == null) {
            return sku.getOriginalPrice();
        }
        if (v.getOriginalPrice() != null) {
            return v.getOriginalPrice();
        }
        return v.getPrice() != null ? v.getPrice() : sku.getOriginalPrice();
    }

    private String calculateDiscountLabel(BigDecimal flash, BigDecimal original) {
        // 1. 參數防禦：任一價格為空或原價非正數時返回空標籤
        if (flash == null || original == null || original.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        // 2. 計算折數百分比：(1 - 秒殺價/原價) × 100
        try {
            BigDecimal ratio = flash.divide(original, 4, RoundingMode.HALF_UP);
            // 3. 轉換為整數百分比並組裝折扣標籤
            int off = BigDecimal.ONE.subtract(ratio)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            return off > 0 ? "-" + off + "%" : "";
        } catch (Exception e) {
            log.warn("【特賣服務】計算折扣標籤失敗: flash={}, original={}", flash, original);
            return "";
        }
    }

    /**
     * Entity 轉 DTO。
     */
    private FlashSalePromotionDTO toDTO(FlashSalePromotion entity) {
        FlashSalePromotionDTO dto = new FlashSalePromotionDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private FlashSaleSessionVO buildSessionVO(FlashSalePromotion p, List<FlashSaleProductVO> products,
            LocalDateTime now) {
        String statusText = "已結束";
        long countdown = 0;

        if (now.isBefore(p.getStartTime())) {
            statusText = "即將開始";
            countdown = Duration.between(now, p.getStartTime()).getSeconds();
        } else if (now.isBefore(p.getEndTime())) {
            statusText = "進行中";
            countdown = Duration.between(now, p.getEndTime()).getSeconds();
        }

        return FlashSaleSessionVO.builder()
                .id(p.getId())
                .name(p.getName())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .statusText(statusText)
                .bannerImage(p.getBannerImage())
                .countdownSeconds(Math.max(0, countdown))
                .products(products)
                .build();
    }
}
