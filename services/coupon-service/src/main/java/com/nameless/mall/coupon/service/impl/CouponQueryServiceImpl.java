package com.nameless.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nameless.mall.coupon.api.vo.CouponCardVO;
import com.nameless.mall.coupon.entity.CouponTemplate;
import com.nameless.mall.coupon.entity.UserCoupon;
import com.nameless.mall.coupon.mapper.CouponTemplateMapper;
import com.nameless.mall.coupon.mapper.UserCouponMapper;
import com.nameless.mall.coupon.service.CouponQueryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nameless.mall.coupon.api.dto.CouponTemplateDTO;
import com.nameless.mall.coupon.api.dto.UserCouponDTO;
import com.nameless.mall.coupon.api.vo.CouponTemplateVO;
import com.nameless.mall.coupon.api.vo.UserCouponVO;
import com.nameless.mall.coupon.service.CouponService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;

/**
 * 優惠券查詢服務實作
 * <p>
 * 負責只讀與 VO 的整合轉換。
 */
@Service
public class CouponQueryServiceImpl implements CouponQueryService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponService couponService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM.dd");

    public CouponQueryServiceImpl(CouponTemplateMapper couponTemplateMapper,
            UserCouponMapper userCouponMapper,
            @Lazy CouponService couponService) {
        this.couponTemplateMapper = couponTemplateMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponService = couponService;
    }

    @Override
    public List<CouponCardVO> getCouponCards(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 查詢所有啟用中且未過期的範本
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, 1) // 啟用
                        .ge(CouponTemplate::getEndTime, now) // 未過期
                        .orderByDesc(CouponTemplate::getId)); // 最新優先

        // 2. 查詢用戶已領取記錄 (若已登入)
        Map<Long, Integer> userClaimCountMap = new HashMap<>();
        if (userId != null) {
            List<UserCoupon> myCoupons = userCouponMapper.selectList(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId));

            // 統計每張券領了幾次: Map<TemplateId, Count>
            for (UserCoupon uc : myCoupons) {
                userClaimCountMap.merge(uc.getTemplateId(), 1, Integer::sum);
            }
        }

        // 3. 轉換 VO
        return templates.stream()
                .map(t -> convertToVO(t, userClaimCountMap.getOrDefault(t.getId(), 0)))
                .collect(Collectors.toList());
    }

    /**
     * VO 轉換核心邏輯
     */
    private CouponCardVO convertToVO(CouponTemplate t, int claimedCount) {
        boolean isClaimMax = claimedCount >= t.getPerUserLimit();
        LocalDateTime now = LocalDateTime.now();

        // 1. 計算狀態文本
        String statusText;
        boolean isClaimable = false;

        if (isClaimMax) {
            statusText = "已領取";
        } else if (t.getRemainCount() <= 0) {
            statusText = "已搶光";
        } else if (now.isBefore(t.getStartTime())) {
            statusText = "即將開始";
        } else {
            statusText = "立即領取";
            isClaimable = true;
        }

        // 2. 格式化金額顯示
        String amountDisplay;
        if (t.getType() == 1) { // 滿減
            amountDisplay = "$" + t.getDiscount().stripTrailingZeros().toPlainString();
        } else if (t.getType() == 2) { // 折扣
            // 0.9 -> 9折, 0.85 -> 85折
            BigDecimal rate = t.getDiscount().multiply(BigDecimal.valueOf(10));
            amountDisplay = rate.stripTrailingZeros().toPlainString() + "折";
        } else {
            amountDisplay = "免運";
        }

        // 3. 門檻顯示
        String thresholdDisplay;
        if (t.getThreshold().compareTo(BigDecimal.ZERO) > 0) {
            thresholdDisplay = "滿 $" + t.getThreshold().stripTrailingZeros().toPlainString() + " 可用";
        } else {
            thresholdDisplay = "無門檻";
        }

        // 4. 有效期描述
        String validityPeriod;
        if (t.getValidType() != null && t.getValidType() == 2) {
            validityPeriod = "領取後 " + t.getValidDays() + " 天內有效";
        } else {
            validityPeriod = t.getStartTime().format(DATE_FMT) + " - " + t.getEndTime().format(DATE_FMT);
        }

        // 5. 進度條 (避免除以0)
        int progress = 0;
        if (t.getTotalCount() > 0) {
            int sold = t.getTotalCount() - t.getRemainCount();
            progress = (int) ((sold * 100.0) / t.getTotalCount());
        }

        return CouponCardVO.builder()
                .id(t.getId())
                .templateId(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .amountDisplay(amountDisplay)
                .thresholdDisplay(thresholdDisplay)
                .statusText(statusText)
                .isClaimable(isClaimable)
                .progress(progress)
                .validityPeriod(validityPeriod)
                .endTime(t.getEndTime())
                .build();
    }

    @Override
    public List<CouponTemplateVO> getAvailableTemplatesVO() {
        List<CouponTemplateDTO> dtos = couponService.getAvailableTemplates();
        return dtos.stream().map(dto -> {
            CouponTemplateVO vo = new CouponTemplateVO();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<UserCouponVO> getMyCouponsVO(
            Long userId, Integer pageNum, Integer pageSize) {
        Page<UserCouponDTO> dtoPage = couponService.getMyCoupons(userId, pageNum, pageSize);
        Page<UserCouponVO> voPage = new Page<>(dtoPage.getCurrent(), dtoPage.getSize(), dtoPage.getTotal());

        List<UserCouponVO> voList = dtoPage.getRecords().stream().map(dto -> {
            UserCouponVO vo = new UserCouponVO();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<UserCouponVO> getUsableCouponsVO(Long userId, BigDecimal amount) {
        List<UserCouponDTO> dtos = couponService.getUsableCoupons(userId, amount);
        return dtos.stream().map(dto -> {
            UserCouponVO vo = new UserCouponVO();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
