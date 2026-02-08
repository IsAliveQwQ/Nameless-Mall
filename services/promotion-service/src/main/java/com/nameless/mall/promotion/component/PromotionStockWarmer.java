package com.nameless.mall.promotion.component;

import com.nameless.mall.promotion.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 特賣庫存預熱器。
 * 應用啟動後自動同步 active 特賣庫存到 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionStockWarmer implements ApplicationRunner {

    private final FlashSaleService flashSaleService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("【系統啟動】開始預熱特賣庫存...");
        try {
            flashSaleService.syncStock();
            log.info("【系統啟動】特賣庫存預熱完成。");
        } catch (Exception e) {
            log.error("【系統啟動】特賣庫存預熱失敗 (非阻斷性錯誤)", e);
        }
    }
}
