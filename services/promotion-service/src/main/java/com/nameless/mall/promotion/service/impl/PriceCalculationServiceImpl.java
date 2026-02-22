package com.nameless.mall.promotion.service.impl;

import com.nameless.mall.promotion.api.dto.MarketingCampaignDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import com.nameless.mall.promotion.api.enums.PromotionType;
import com.nameless.mall.promotion.api.vo.FlashSaleProductVO;
import com.nameless.mall.promotion.api.vo.FlashSaleSessionVO;
import com.nameless.mall.promotion.enums.CampaignStatus;

import com.nameless.mall.promotion.service.FlashSalePromotionService;
import com.nameless.mall.promotion.service.MarketingCampaignService;
import com.nameless.mall.promotion.service.PriceCalculationService;
import com.nameless.mall.promotion.service.PromotionCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 促銷計價引擎實作。
 * 負責彙整秒殺與行銷活動，計算商品的最優優惠價格。
 *
 * 效能設計：閃購 session 與行銷活動採用本地快取 (volatile + TTL)，
 * 避免高併發下每次 calculateBestPrices 都重複執行 DB 查詢 + Feign 呼叫。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationServiceImpl implements PriceCalculationService {

    private static final Long GLOBAL_CATEGORY_ID = -1L;

    /** 閃購 session 快取 TTL (10 秒，平衡即時性與效能) */
    private static final long SESSION_CACHE_TTL_MS = 10_000L;
    /** 行銷活動快取 TTL (30 秒，活動變更頻率低) */
    private static final long CAMPAIGN_CACHE_TTL_MS = 30_000L;

    private volatile FlashSaleSessionVO cachedSession;
    private volatile long sessionCacheExpiry = 0;

    private volatile List<MarketingCampaignDTO> cachedCampaigns;
    private volatile long campaignCacheExpiry = 0;

    private final FlashSalePromotionService flashSaleService;
    private final MarketingCampaignService campaignService;
    private final PromotionCacheManager cacheManager;

    @Override
    public List<ProductPriceResultDTO> calculateBestPrices(List<ProductPriceCheckDTO> checkList) {
        if (checkList == null || checkList.isEmpty()) {
            return List.of();
        }

        // 1. 取得當前有效的閃購場次（含本地快取）
        FlashSaleSessionVO currentSession = getActiveFlashSaleSession();
        // 2. 建構閃購商品快速查找表 (variantId → VO)
        Map<Long, FlashSaleProductVO> flashSaleMap = buildFlashSaleMap(currentSession);

        // 3. 取得有效行銷活動並按分類分組
        Map<Long, List<MarketingCampaignDTO>> campaignMap = getActiveMarketingCampaigns().stream()
                .collect(
                        Collectors.groupingBy(c -> c.getCategoryId() == null ? GLOBAL_CATEGORY_ID : c.getCategoryId()));

        // 4. 逐一計算每個商品的最優價格（閃購 > 行銷活動 > 原價）
        return checkList.stream()
                .map(item -> calculateSingle(item, flashSaleMap, currentSession, campaignMap))
                .collect(Collectors.toList());
    }

    private ProductPriceResultDTO calculateSingle(
            ProductPriceCheckDTO item,
            Map<Long, FlashSaleProductVO> flashSaleMap,
            FlashSaleSessionVO session,
            Map<Long, List<MarketingCampaignDTO>> campaignMap) {

        // 1. 優先級判定：秒殺 (不可與其他活動疊加)
        if (flashSaleMap.containsKey(item.getVariantId())) {
            FlashSaleProductVO f = flashSaleMap.get(item.getVariantId());
            if (f.getFlashPrice() != null) {
                BigDecimal base = f.getOriginalPrice() != null
                        ? f.getOriginalPrice()
                        : item.getOriginalPrice();
                return buildResult(
                        item.getVariantId(),
                        base,
                        f.getFlashPrice(),
                        PromotionType.FLASH_SALE.name(),
                        session.getId(),
                        session.getName());
            }
        }

        // 2. 搜尋最優行銷活動
        return findBestCampaign(item, campaignMap);
    }

    private ProductPriceResultDTO findBestCampaign(
            ProductPriceCheckDTO item,
            Map<Long, List<MarketingCampaignDTO>> campaignMap) {

        // 1. 取得原價，若為空則直接返回無折扣結果
        BigDecimal originalPrice = item.getOriginalPrice();
        if (originalPrice == null) {
            return buildNoneResult(item.getVariantId(), BigDecimal.ZERO);
        }

        // 2. 彙整候選活動（全域 + 沿分類路徑向上匹配）
        List<MarketingCampaignDTO> candidates = new ArrayList<>(
                campaignMap.getOrDefault(GLOBAL_CATEGORY_ID, List.of()));

        Map<Long, Long> parents = cacheManager.getCategoryParentMap();
        Long cur = item.getCategoryId();
        int safeDepth = 0;
        while (cur != null && safeDepth++ < 10) {
            candidates.addAll(campaignMap.getOrDefault(cur, List.of()));
            cur = parents.get(cur);
        }

        // 3. 從候選中找出折扣最深的活動並計算最終價格
        return candidates.stream()
                .filter(this::isDiscountValid)
                .min(Comparator.comparing(MarketingCampaignDTO::getDiscountRate))
                .map(c -> {
                    BigDecimal finalPrice = originalPrice
                            .multiply(c.getDiscountRate())
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    return buildResult(
                            item.getVariantId(),
                            originalPrice,
                            finalPrice,
                            PromotionType.CAMPAIGN.name(),
                            c.getId(),
                            c.getTitle());
                })
                .orElseGet(() -> buildNoneResult(item.getVariantId(), originalPrice));
    }

    private ProductPriceResultDTO buildResult(
            Long variantId,
            BigDecimal original,
            BigDecimal finalPrice,
            String type,
            Long id,
            String name) {
        return ProductPriceResultDTO.builder()
                .variantId(variantId)
                .originalPrice(original)
                .finalPrice(finalPrice)
                .discountAmount(original.subtract(finalPrice))
                .promotionType(type)
                .promotionId(id)
                .promotionName(name)
                .build();
    }

    private ProductPriceResultDTO buildNoneResult(Long variantId, BigDecimal price) {
        return buildResult(variantId, price, price, PromotionType.NONE.name(), null, null);
    }

    private boolean isDiscountValid(MarketingCampaignDTO c) {
        return c.getDiscountRate() != null
                && c.getDiscountRate().compareTo(BigDecimal.ONE) < 0
                && c.getDiscountRate().compareTo(BigDecimal.ZERO) > 0;
    }

    private FlashSaleSessionVO getActiveFlashSaleSession() {
        long now = System.currentTimeMillis();
        // 1. 檢查本地快取是否仍有效
        if (cachedSession != null && now < sessionCacheExpiry) {
            return cachedSession;
        }
        try {
            // 2. 快取過期，重新查詢當前閃購場次
            FlashSaleSessionVO session = flashSaleService.getCurrentSession();
            // 3. 驗證場次時間窗口並更新快取
            LocalDateTime ldt = LocalDateTime.now();
            if (session != null
                    && session.getProducts() != null
                    && ldt.isAfter(session.getStartTime())
                    && ldt.isBefore(session.getEndTime())) {
                cachedSession = session;
                sessionCacheExpiry = now + SESSION_CACHE_TTL_MS;
                return session;
            }
            cachedSession = null;
            sessionCacheExpiry = now + SESSION_CACHE_TTL_MS;
        } catch (Exception e) {
            log.warn("【計價引擎】獲取當前秒殺場次失敗或無活動: {}", e.getMessage());
            // 快取失敗結果 5 秒，避免雪崩
            sessionCacheExpiry = now + 5_000L;
        }
        return cachedSession;
    }

    private List<MarketingCampaignDTO> getActiveMarketingCampaigns() {
        long now = System.currentTimeMillis();
        if (cachedCampaigns != null && now < campaignCacheExpiry) {
            return cachedCampaigns;
        }
        try {
            // 2. 獲取並過濾目前有效的所有行銷活動
            List<MarketingCampaignDTO> campaigns = campaignService.getActiveCampaigns();
            if (campaigns == null || campaigns.isEmpty()) {
                return Collections.emptyList();
            }

            // 僅保留狀態進行中且在時間內的活動
            List<MarketingCampaignDTO> activeCampaigns = campaigns.stream()
                    .filter(this::isCampaignActive)
                    .collect(Collectors.toList());
            cachedCampaigns = activeCampaigns;
            campaignCacheExpiry = now + CAMPAIGN_CACHE_TTL_MS;
            return activeCampaigns;
        } catch (Exception e) {
            log.error("【計價引擎】獲取行銷活動列表異常", e);
            campaignCacheExpiry = now + 5_000L;
            return Collections.emptyList();
        }
    }

    private Map<Long, FlashSaleProductVO> buildFlashSaleMap(FlashSaleSessionVO session) {
        if (session == null || session.getProducts() == null) {
            return Collections.emptyMap();
        }
        return session.getProducts().stream()
                .filter(p -> p.getVariantId() != null)
                .collect(Collectors.toMap(FlashSaleProductVO::getVariantId, p -> p, (a, b) -> a));
    }

    /**
     * 檢查行銷活動是否進行中 (非草稿、非暫停且時間吻合)。
     */
    private boolean isCampaignActive(MarketingCampaignDTO campaign) {
        if (campaign == null)
            return false;
        if (CampaignStatus.DRAFT.name().equals(campaign.getStatus()) ||
                CampaignStatus.SUSPENDED.name().equals(campaign.getStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStartTime() != null && now.isBefore(campaign.getStartTime())) {
            return false; // 尚未開始
        }
        if (campaign.getEndTime() != null && now.isAfter(campaign.getEndTime())) {
            return false; // 已結束
        }
        return true;
    }
}
