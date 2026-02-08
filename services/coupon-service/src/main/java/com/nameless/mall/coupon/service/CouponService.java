package com.nameless.mall.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.coupon.api.dto.CouponCalculationDTO;
import com.nameless.mall.coupon.api.dto.CouponCalculationResult;
import com.nameless.mall.coupon.api.dto.CouponTemplateDTO;
import com.nameless.mall.coupon.api.dto.UserCouponDTO;
import com.nameless.mall.coupon.api.vo.ApplicableCouponVO;
import com.nameless.mall.coupon.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 優惠券服務介面
 */
public interface CouponService extends IService<UserCoupon> {

        /**
         * 獲取可領取的優惠券列表
         */
        List<CouponTemplateDTO> getAvailableTemplates();

        /**
         * 領取優惠券
         */
        void claimCoupon(Long templateId, Long userId);

        /**
         * 獲取用戶的優惠券列表
         */
        Page<UserCouponDTO> getMyCoupons(Long userId, Integer pageNum, Integer pageSize);

        /**
         * 獲取用戶可用於訂單的優惠券
         */
        List<UserCouponDTO> getUsableCoupons(Long userId, BigDecimal amount);

        /**
         * 核銷優惠券
         */
        void useCoupon(Long userCouponId, String orderSn);

        /**
         * 退還優惠券
         */
        void returnCoupon(Long userCouponId);

        /**
         * 為新用戶發放優惠券
         */
        void distributeNewUserCoupon(Long userId);

        /**
         * [New] 試算優惠券折扣
         */
        CouponCalculationResult calculateDiscount(
                        CouponCalculationDTO dto);

        /**
         * [New]獲取訂單可用的優惠券列表 (含試算結果)
         */
        List<ApplicableCouponVO> getApplicableCoupons(
                        CouponCalculationDTO dto);
}
