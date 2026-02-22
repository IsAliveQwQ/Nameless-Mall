package com.nameless.mall.promotion.startup;

import com.nameless.mall.promotion.service.FlashSalePromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 特賣活動數據一致性檢查 Runner
 * <p>
 * 在服務啟動時，檢查所有進行中的特賣活動，
 * 並確保所有相關商品的規格都已正確寫入 flash_sale_skus 表。
 * 這解決了因資料種子或管理員操作不完整導致的規格缺失問題。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleStartupRunner implements ApplicationRunner {

    private final FlashSalePromotionService flashSalePromotionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("【系統啟動】開始執行特賣活動數據一致性檢查...");
        try {
            flashSalePromotionService.syncPromotionStock();
            log.info("【系統啟動】特賣活動數據一致性檢查完成。");
        } catch (Exception e) {
            log.error("【系統啟動】特賣活動數據一致性檢查失敗", e);
            // 不阻斷啟動，但記錄錯誤
        }
    }
}
