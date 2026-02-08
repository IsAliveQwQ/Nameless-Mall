package com.nameless.mall.coupon.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.coupon.api.dto.CouponCalculationDTO;
import com.nameless.mall.coupon.api.dto.CouponCalculationResult;
import com.nameless.mall.coupon.api.dto.CouponUseInputDTO;
import com.nameless.mall.coupon.api.vo.ApplicableCouponVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 優惠券服務的 Feign 客戶端接口
 */
@FeignClient(name = "coupon-service", fallbackFactory = CouponFeignFallback.class)
public interface CouponFeignClient {

        /**
         * 核銷優惠券 (訂單服務內部呼叫)
         */
        @PostMapping("/coupons/internal/use")
        Result<Void> useCoupon(@RequestBody CouponUseInputDTO dto);

        /**
         * 退還優惠券 (訂單取消時呼叫)
         */
        @PostMapping("/coupons/internal/return")
        Result<Void> returnCoupon(@RequestParam("userCouponId") Long userCouponId);

        /**
         * 試算優惠券折扣
         */
        @PostMapping("/coupons/internal/calculate")
        Result<CouponCalculationResult> calculateDiscount(
                        @RequestBody CouponCalculationDTO dto);

        /**
         * 獲取訂單可用的優惠券列表
         */
        @PostMapping("/coupons/internal/applicable")
        Result<List<ApplicableCouponVO>> getApplicableCoupons(
                        @RequestBody CouponCalculationDTO dto);
}
