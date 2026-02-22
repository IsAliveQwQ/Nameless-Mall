package com.nameless.mall.coupon.service;

import com.nameless.mall.coupon.api.vo.CouponCardVO;
import java.util.List;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.coupon.api.vo.CouponTemplateVO;
import com.nameless.mall.coupon.api.vo.UserCouponVO;

/**
 * 優惠券查詢服務
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

        /**
         * 獲取可領取的優惠券範本列表
         */
        List<CouponTemplateVO> getAvailableTemplatesVO();

        /**
         * 獲取用戶的優惠券列表
         */
        Page<UserCouponVO> getMyCouponsVO(Long userId, Integer pageNum, Integer pageSize);

        /**
         * 獲取用戶可用於訂單的優惠券
         */
        List<UserCouponVO> getUsableCouponsVO(Long userId, BigDecimal amount);
}
