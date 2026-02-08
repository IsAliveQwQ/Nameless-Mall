package com.nameless.mall.coupon.service;

import com.nameless.mall.coupon.api.vo.CouponCardVO;
import java.util.List;

/**
 * 優惠券查詢服務 (CQRS 讀模型)
 * <p>
 * 負責聚合查詢，不涉及業務邏輯變更。
 * 設計目的：解除 CouponService <-> CouponTemplateService 的循環依賴。
 */
public interface CouponQueryService {

    /**
     * 獲取優惠券卡片列表 (前端展示用)
     * <p>
     * 聚合模板資訊與用戶領取記錄，計算每張券的顯示狀態。
     *
     * @param userId 當前用戶ID (可為 null，表示未登入)
     * @return 前端專用 VO 列表
     */
    List<CouponCardVO> getCouponCards(Long userId);
}
