package com.nameless.mall.promotion.config;

import com.nameless.mall.promotion.entity.FlashSalePromotion;
import com.nameless.mall.promotion.entity.FlashSaleSku;
import com.nameless.mall.promotion.service.FlashSalePromotionService;
import com.nameless.mall.promotion.service.FlashSaleService;
import com.nameless.mall.promotion.mapper.FlashSaleSkuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 測試數據初始化器。
 * 僅在 dev profile 啟用，應用啟動時自動寫入範例特賣活動與商品，
 * 並觸發 Redis 庫存預熱以確保新寫入的數據可正常使用。
 */
@Slf4j
@Component
@Profile("dev")
@Order(1) // 確保在 PromotionStockWarmer(ApplicationRunner) 之前執行
public class FlashSaleDataInitializer implements CommandLineRunner {

    private final FlashSalePromotionService promotionService;
    private final FlashSaleSkuMapper skuMapper;
    private final FlashSaleService flashSaleService;

    public FlashSaleDataInitializer(FlashSalePromotionService promotionService,
                                     FlashSaleSkuMapper skuMapper,
                                     FlashSaleService flashSaleService) {
        this.promotionService = promotionService;
        this.skuMapper = skuMapper;
        this.flashSaleService = flashSaleService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("【數據初始化】檢查限時特賣數據...");

        // 檢查是否存在進行中的活動
        if (promotionService.getCurrentSession() != null) {
            log.info("【數據初始化】已有進行中活動，跳過初始化。");
            return;
        }

        log.info("【數據初始化】未發現進行中活動，開始寫入測試數據...");

        // 1. 建立特賣活動
        FlashSalePromotion promotion = new FlashSalePromotion();
        promotion.setName("春節限時特賣");
        promotion.setBannerImage("https://placehold.co/1200x400/BF3939/FFFFFF?text=Spring+Sale");
        promotion.setDescription("限時搶購，低至5折！");
        // 設定為：前一小時開始，後兩小時結束
        promotion.setStartTime(LocalDateTime.now().minusHours(1));
        promotion.setEndTime(LocalDateTime.now().plusHours(2));
        promotion.setStatus(1); // 啟用
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());

        boolean saved = promotionService.save(promotion);
        if (!saved) {
            log.error("【數據初始化】活動寫入失敗");
            return;
        }
        log.info("【數據初始化】活動寫入成功: ID={}", promotion.getId());

        // 2. 建立特賣商品 (假設已有一些商品 ID)
        // 注意：這裡需要真實存在的 productId 和 variantId 才能在前端正確導航
        // 使用常見的測試 ID (需根據實際 DB 資料調整，這是有風險的，但本地開發通常 ID 是 1, 2...)
        // 假設 Product ID=1, Variant ID=1
        createSku(promotion.getId(), 1L, 1L, new BigDecimal("1200"), new BigDecimal("999"), 100);

        // 假設 Product ID=2, Variant ID=2
        createSku(promotion.getId(), 2L, 2L, new BigDecimal("2500"), new BigDecimal("1888"), 50);

        // 假設 Product ID=3, Variant ID=3
        createSku(promotion.getId(), 3L, 3L, new BigDecimal("800"), new BigDecimal("500"), 20);

        log.info("【數據初始化】測試數據寫入完成！");

        // 寫入完後觸發 Redis 庫存預熱，確保 dev 環境下特賣可正常使用
        try {
            flashSaleService.syncStock();
            log.info("【數據初始化】Redis 庫存預熱完成。");
        } catch (Exception e) {
            log.warn("【數據初始化】Redis 庫存預熱失敗（非阻斷）", e);
        }
    }

    private void createSku(Long promotionId, Long productId, Long variantId, BigDecimal original, BigDecimal flash,
            Integer stock) {
        FlashSaleSku sku = new FlashSaleSku();
        sku.setPromotionId(promotionId);
        sku.setProductId(productId);
        sku.setVariantId(variantId);
        sku.setOriginalPrice(original);
        sku.setFlashSalePrice(flash);
        sku.setFlashSaleStock(stock);
        sku.setFlashSaleLimit(100);
        sku.setLimitPerUser(2);
        sku.setSoldCount(0);
        skuMapper.insert(sku);
    }
}
