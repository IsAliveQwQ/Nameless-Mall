package com.nameless.mall.coupon.api.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.coupon.api.dto.CouponCalculationDTO;
import com.nameless.mall.coupon.api.dto.CouponCalculationResult;
import com.nameless.mall.coupon.api.dto.CouponUseInputDTO;
import com.nameless.mall.coupon.api.vo.ApplicableCouponVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CouponFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class CouponFeignFallback implements FallbackFactory<CouponFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(CouponFeignFallback.class);

    @Override
    public CouponFeignClient create(Throwable cause) {
        return new CouponFeignClient() {
            @Override
            public Result<Void> useCoupon(CouponUseInputDTO dto) {
                log.error("降級 | CouponFeignClient.useCoupon 失敗, userCouponId: {}, cause: {}",
                        dto != null ? dto.getUserCouponId() : "null", cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "優惠券服務暫時不可用");
            }

            @Override
            public Result<Void> returnCoupon(Long userCouponId) {
                log.error("降級 | CouponFeignClient.returnCoupon 失敗, userCouponId: {}, cause: {}",
                        userCouponId, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "優惠券服務暫時不可用，返還將稍後重試");
            }

            @Override
            public Result<CouponCalculationResult> calculateDiscount(CouponCalculationDTO dto) {
                log.error("降級 | CouponFeignClient.calculateDiscount 失敗, cause: {}",
                        cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "優惠券服務暫時不可用，無法計算折扣");
            }

            @Override
            public Result<List<ApplicableCouponVO>> getApplicableCoupons(CouponCalculationDTO dto) {
                log.error("降級 | CouponFeignClient.getApplicableCoupons 失敗, cause: {}",
                        cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "優惠券服務暫時不可用");
            }
        };
    }
}
