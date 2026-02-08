package com.nameless.mall.promotion.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.promotion.client.ProductClient;
import com.nameless.mall.promotion.entity.FlashSalePromotion;
import com.nameless.mall.promotion.entity.FlashSaleSku;
import com.nameless.mall.promotion.mapper.FlashSaleSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 特賣商品同步管理器。
 * 負責從 Product Service 同步最新的商品規格資料至特賣場次中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleSyncManager {

    private final FlashSaleSkuMapper skuMapper;
    private final ProductClient productClient;

    /**
     * 同步指定活動的商品 SKU 數據。
     */
    public void syncPromotion(FlashSalePromotion promotion) {
        log.info("開始同步特賣活動資料: id={}, name={}", promotion.getId(), promotion.getName());

        // 1. 查詢該活動目前已有的特賣 SKU
        List<FlashSaleSku> existing = skuMapper.selectList(
                new LambdaQueryWrapper<FlashSaleSku>().eq(FlashSaleSku::getPromotionId, promotion.getId()));

        if (existing.isEmpty())
            return;

        // 2. 透過 Feign 拉取最新商品規格清單
        List<VariantDTO> variants = fetchVariants(existing);
        if (variants == null || variants.isEmpty())
            return;

        // 3. 過濾出尚未加入特賣的新規格並構建 SKU
        Set<Long> existingIds = existing.stream().map(FlashSaleSku::getVariantId).collect(Collectors.toSet());

        List<FlashSaleSku> newSkus = variants.stream()
                .filter(v -> !existingIds.contains(v.getId()))
                .map(v -> buildNewSku(promotion.getId(), v, calculateDiscountRate(existing.get(0))))
                .collect(Collectors.toList());

        // 4. 批量寫入新特賣 SKU
        if (!newSkus.isEmpty()) {
            batchInsert(newSkus, promotion.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(List<FlashSaleSku> skus, Long promotionId) {
        skus.forEach(sku -> {
            try {
                skuMapper.insert(sku);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.debug("跳過重複 SKU: variantId={}", sku.getVariantId());
            }
        });
    }

    private List<VariantDTO> fetchVariants(List<FlashSaleSku> existing) {
        try {
            List<Long> productIds = existing.stream().map(FlashSaleSku::getProductId).distinct()
                    .collect(Collectors.toList());
            Result<List<VariantDTO>> res = productClient.getVariantsByProductIds(productIds);
            return (res != null) ? res.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("透過 Feign 獲取商品規格失敗", e);
            return Collections.emptyList();
        }
    }

    /**
     * 構建新特賣 SKU。
     */
    private FlashSaleSku buildNewSku(Long promoId, VariantDTO v, BigDecimal rate) {
        return FlashSaleSku.builder()
                .promotionId(promoId)
                .productId(v.getProductId())
                .variantId(v.getId())
                .originalPrice(v.getPrice())
                .flashSalePrice(v.getPrice().multiply(rate).setScale(2, RoundingMode.HALF_UP))
                .flashSaleStock(100)
                .flashSaleLimit(10)
                .limitPerUser(2)
                .soldCount(0)
                .build();
    }

    private BigDecimal calculateDiscountRate(FlashSaleSku template) {
        if (template.getOriginalPrice() == null || template.getOriginalPrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(0.85); // 預設 85 折
        }
        return template.getFlashSalePrice().divide(template.getOriginalPrice(), 2, RoundingMode.HALF_UP);
    }
}
