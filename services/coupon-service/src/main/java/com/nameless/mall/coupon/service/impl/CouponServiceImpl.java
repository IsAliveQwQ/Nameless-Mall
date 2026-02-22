package com.nameless.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.coupon.api.dto.CouponTemplateDTO;
import com.nameless.mall.coupon.api.dto.UserCouponDTO;

import com.nameless.mall.coupon.entity.UserCoupon;
import com.nameless.mall.coupon.mapper.UserCouponMapper;
import com.nameless.mall.coupon.service.CouponService;
import com.nameless.mall.coupon.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nameless.mall.coupon.api.dto.CouponCalculationDTO;
import com.nameless.mall.coupon.api.dto.CouponCalculationResult;
import com.nameless.mall.coupon.api.vo.ApplicableCouponVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * 優惠券服務實作
 * <p>
 * 負責處理用戶優惠券相關的業務邏輯，包括領取、使用、退還等。
 * 優惠券範本相關操作委派給 CouponTemplateService。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements CouponService {

    private final CouponTemplateService couponTemplateService;

    @Override
    public List<CouponTemplateDTO> getAvailableTemplates() {
        // 委派給 CouponTemplateService
        return couponTemplateService.getAvailableTemplates();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void claimCoupon(Long templateId, Long userId) {
        // 透過 CouponTemplateService 驗證範本
        couponTemplateService.validateAndGet(templateId);

        // 檢查用戶是否已領取
        Long count = this.count(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getTemplateId, templateId));
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_ALREADY_CLAIMED);
        }

        // 透過 CouponTemplateService 扣減剩餘數量（原子操作）
        couponTemplateService.decreaseRemainCount(templateId);

        // 創建用戶優惠券（UNIQUE(user_id, template_id) 兜底防並發）
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setStatus(0);
        userCoupon.setCreatedAt(LocalDateTime.now());
        try {
            this.save(userCoupon);
        } catch (DuplicateKeyException e) {
            log.warn("【競態保護】優惠券重複領取被 UNIQUE 約束攔截: userId={}, templateId={}", userId, templateId);
            throw new BusinessException(ResultCodeEnum.COUPON_ALREADY_CLAIMED);
        }
    }

    @Override
    public Page<UserCouponDTO> getMyCoupons(Long userId, Integer pageNum, Integer pageSize) {
        // 1. 分頁查詢用戶的優惠券記錄
        Page<UserCoupon> page = new Page<>(pageNum, pageSize);
        Page<UserCoupon> userCouponPage = this.page(page,
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .orderByDesc(UserCoupon::getCreatedAt));

        List<UserCoupon> records = userCouponPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new Page<>(userCouponPage.getCurrent(), userCouponPage.getSize(), userCouponPage.getTotal());
        }

        // 2. 蒐集不重複的範本 ID，批量查詢範本資訊（防止 N+1）
        List<Long> templateIds = records.stream()
                .map(UserCoupon::getTemplateId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CouponTemplateDTO> templateMap = couponTemplateService.getTemplatesBatch(templateIds)
                .stream()
                .collect(Collectors.toMap(CouponTemplateDTO::getId, t -> t, (v1, v2) -> v1));

        // 3. 逐筆組裝 DTO，合併範本欄位
        List<UserCouponDTO> dtos = records.stream().map(userCoupon -> {
            UserCouponDTO dto = new UserCouponDTO();
            BeanUtils.copyProperties(userCoupon, dto);

            CouponTemplateDTO template = templateMap.get(userCoupon.getTemplateId());
            if (template != null) {
                dto.setCouponName(template.getName());
                dto.setType(template.getType());
                dto.setThreshold(template.getThreshold());
                dto.setDiscount(template.getDiscount());
                dto.setExpireTime(template.getEndTime());
            }
            return dto;
        }).collect(Collectors.toList());

        // 4. 封裝分頁結果並回傳
        Page<UserCouponDTO> dtoPage = new Page<>(userCouponPage.getCurrent(), userCouponPage.getSize(),
                userCouponPage.getTotal());
        dtoPage.setRecords(dtos);
        return dtoPage;
    }

    @Override
    public List<UserCouponDTO> getUsableCoupons(Long userId, BigDecimal amount) {
        List<UserCoupon> userCoupons = this.list(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0));

        if (userCoupons.isEmpty())
            return List.of();

        // 批量查詢範本（防止 N+1）
        List<Long> templateIds = userCoupons.stream()
                .map(UserCoupon::getTemplateId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CouponTemplateDTO> templateMap = couponTemplateService.getTemplatesBatch(templateIds)
                .stream()
                .collect(Collectors.toMap(CouponTemplateDTO::getId, t -> t, (v1, v2) -> v1));

        return userCoupons.stream()
                .map(userCoupon -> {
                    CouponTemplateDTO template = templateMap.get(userCoupon.getTemplateId());
                    if (template == null) {
                        return null;
                    }

                    // 檢查是否過期（依 validType 區分券型）
                    LocalDateTime expireTime = resolveExpireTime(template, userCoupon);
                    if (LocalDateTime.now().isAfter(expireTime)) {
                        return null;
                    }

                    // 檢查是否滿足使用門檻
                    if (template.getThreshold() != null && amount.compareTo(template.getThreshold()) < 0) {
                        return null;
                    }

                    return buildUserCouponDTOWithTemplate(userCoupon, template);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void useCoupon(Long userCouponId, String orderSn) {
        // 僅當 status=0 (未使用) 時才更新為 status=1 (已使用)
        boolean updated = this.update(
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>()
                        .eq(UserCoupon::getId, userCouponId)
                        .eq(UserCoupon::getStatus, 0)
                        .set(UserCoupon::getStatus, 1)
                        .set(UserCoupon::getOrderSn, orderSn)
                        .set(UserCoupon::getUsedAt, LocalDateTime.now()));

        if (!updated) {
            // CAS 失敗：券不存在或已被使用/過期
            log.warn("【優惠券】CAS 更新失敗，券可能已使用或不存在, userCouponId={}, orderSn={}", userCouponId, orderSn);
            throw new BusinessException(ResultCodeEnum.COUPON_ALREADY_USED, "優惠券已使用或已過期");
        }
        log.debug("【優惠券】核銷成功, userCouponId={}, orderSn={}", userCouponId, orderSn);
    }

    @Override
    public void returnCoupon(Long userCouponId) {
        UserCoupon userCoupon = this.getById(userCouponId);
        if (userCoupon != null && userCoupon.getStatus() == 1) {
            userCoupon.setStatus(0);
            userCoupon.setOrderSn(null);
            userCoupon.setUsedAt(null);
            this.updateById(userCoupon);
        }
    }

    @Value("${coupon.new-user.template-ids:}")
    private String newUserTemplateIds;

    @Override
    public void distributeNewUserCoupon(Long userId) {
        if (newUserTemplateIds == null || newUserTemplateIds.isBlank()) {
            log.info("未配置新人優惠券 (coupon.new-user.template-ids)，跳過發放");
            return;
        }

        String[] ids = newUserTemplateIds.split(",");
        log.info("開始為新用戶 {} 發放優惠券，共 {} 張", userId, ids.length);

        for (String idStr : ids) {
            try {
                Long templateId = Long.parseLong(idStr.trim());
                // 復用領取邏輯，自動進行庫存、重複領取等校驗
                this.claimCoupon(templateId, userId);
                log.info("成功為用戶 {} 發放新人優惠券 {}", userId, templateId);
            } catch (BusinessException e) {
                // 如果已領取或庫存不足，記錄並忽略，不影響其他券的發放
                log.warn("無法為用戶 {} 發放優惠券 {}: {}", userId, idStr, e.getMessage());
            } catch (Exception e) {
                log.error("發放新人優惠券 {} 發生未知錯誤", idStr, e);
            }
        }
    }

    @Override
    public CouponCalculationResult calculateDiscount(
            CouponCalculationDTO dto) {
        // 1. 驗證輸入
        if (dto.getUserCouponId() == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "優惠券 ID 不能為空");
        }
        if (dto.getOrderTotalAmount() == null || dto.getOrderTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "訂單金額無效");
        }

        // 2. 獲取用戶優惠券
        UserCoupon userCoupon = this.getById(dto.getUserCouponId());
        if (userCoupon == null) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_FOUND);
        }

        // 3. 安全校驗
        if (!userCoupon.getUserId().equals(dto.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "無權使用此優惠券");
        }
        if (userCoupon.getStatus() != 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_ALREADY_USED); // 或過期
        }

        // 4. 獲取並校驗模板規則 (有效期、門檻)
        CouponTemplateDTO template = couponTemplateService.getTemplateById(userCoupon.getTemplateId());
        if (template == null) {
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "優惠券模板不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = resolveExpireTime(template, userCoupon);
        if (now.isBefore(template.getStartTime()) || now.isAfter(expireTime)) {
            throw new BusinessException(ResultCodeEnum.COUPON_EXPIRED);
        }

        // 門檻校驗
        if (template.getThreshold() != null && dto.getOrderTotalAmount().compareTo(template.getThreshold()) < 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_AMOUNT_NOT_MET,
                    String.format("未滿 %s 元，無法使用", template.getThreshold()));
        }

        // 5. 計算折扣
        BigDecimal shippingFee = dto.getShippingFee() != null ? dto.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discountAmount = calculateDiscountInternal(template, dto.getOrderTotalAmount(), shippingFee);

        // 6. 構建結果
        CouponCalculationResult result = new CouponCalculationResult();
        result.setDiscountAmount(discountAmount);
        result.setCouponName(template.getName());
        result.setCouponType(template.getType());

        // 計算折後商品總額 (不含運費) -> finalAmount = total - discount
        // 若 Type=3 (免運)，discount 為 shippingFee，則 finalAmount = total (商品沒折)
        if (template.getType() == 3) {
            result.setFinalAmount(dto.getOrderTotalAmount());
        } else {
            BigDecimal finalAmount = dto.getOrderTotalAmount().subtract(discountAmount);
            result.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) > 0 ? finalAmount : BigDecimal.ZERO);
        }

        return result;
    }

    @Override
    public List<ApplicableCouponVO> getApplicableCoupons(
            CouponCalculationDTO dto) {
        BigDecimal shippingFee = dto.getShippingFee() != null ? dto.getShippingFee() : BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        // 1. 查詢用戶所有未使用優惠券
        List<UserCoupon> userCoupons = this.list(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, dto.getUserId())
                .eq(UserCoupon::getStatus, 0));

        if (CollectionUtils.isEmpty(userCoupons)) {
            return new ArrayList<>();
        }

        // 2. 批量查詢模板 (防止 N+1)
        List<Long> templateIds = userCoupons.stream()
                .map(UserCoupon::getTemplateId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CouponTemplateDTO> templateMap = couponTemplateService.getTemplatesBatch(templateIds)
                .stream()
                .collect(Collectors.toMap(CouponTemplateDTO::getId, t -> t, (v1, v2) -> v1));

        // 3. 轉換並試算
        return userCoupons.stream().map(uc -> {
            CouponTemplateDTO template = templateMap.get(uc.getTemplateId());

            // Guard: 防禦 NPE
            if (template == null) {
                log.warn("優惠券模板不存在: templateId={}", uc.getTemplateId());
                return null;
            }

            ApplicableCouponVO vo = new ApplicableCouponVO();
            vo.setId(uc.getId());
            vo.setTemplateId(uc.getTemplateId());
            vo.setStartTime(template.getStartTime());
            vo.setEndTime(template.getEndTime());
            vo.setCouponName(template.getName());
            vo.setType(template.getType());
            vo.setThreshold(template.getThreshold());
            vo.setValue(template.getDiscount());

            // 檢查是否可用
            boolean usable = true;
            String reason = "";

            if (now.isBefore(template.getStartTime())) {
                usable = false;
                reason = "尚未開始";
            } else if (now.isAfter(resolveExpireTime(template, uc))) {
                usable = false;
                reason = "已過期";
            } else if (template.getThreshold() != null
                    && dto.getOrderTotalAmount().compareTo(template.getThreshold()) < 0) {
                usable = false;
                reason = "未滿 " + template.getThreshold();
            }

            vo.setUsable(usable);
            vo.setReason(reason);

            if (usable) {
                vo.setEstimatedDiscount(
                        calculateDiscountInternal(template, dto.getOrderTotalAmount(), shippingFee));
            }
            return vo;
        }).filter(vo -> vo != null).collect(Collectors.toList());
    }

    /**
     * 內部核心算式
     */
    private BigDecimal calculateDiscountInternal(CouponTemplateDTO template, BigDecimal orderTotal,
            BigDecimal shippingFee) {
        BigDecimal discount = BigDecimal.ZERO;

        if (template.getType() == 1) {
            // 滿減 (Cash)
            discount = template.getDiscount(); // e.g. 100
        } else if (template.getType() == 2) {
            // 折扣 (Discount Rate)
            // 假設 template.discount 是 0.9 (9折)
            // 折扣金額 = total * (1 - 0.9) = total * 0.1
            BigDecimal rate = template.getDiscount();
            if (rate != null && rate.compareTo(BigDecimal.ONE) < 0) {
                discount = orderTotal.multiply(BigDecimal.ONE.subtract(rate));
            }
        } else if (template.getType() == 3) {
            // 免運 (Free Shipping)
            discount = shippingFee;
        }

        // maxDiscount 封頂：防止折扣券/滿減券折扣金額超過上限
        // 例如：9 折券 maxDiscount=200，應用於 $10,000 訂單時折扣封頂為 $200 而非 $1,000
        if (template.getMaxDiscount() != null
                && template.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(template.getMaxDiscount()) > 0) {
            log.debug("【優惠券】折扣封頂生效, 原始折扣={}, maxDiscount={}", discount, template.getMaxDiscount());
            discount = template.getMaxDiscount();
        }

        // 防禦：折扣不能超過訂單金額 (Type=3 除外，因它抵運費)
        if (template.getType() != 3 && discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        // 精度處理 (四捨五入到小數點後2位)
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 根據券型計算實際過期時間。
     * <ul>
     * <li>validType=1 (固定日期)：使用 template.endTime</li>
     * <li>validType=2 (領取後 N 天)：使用 userCoupon.createdAt + validDays</li>
     * <li>其他 / null：fallback 為 template.endTime</li>
     * </ul>
     */
    private LocalDateTime resolveExpireTime(CouponTemplateDTO template, UserCoupon userCoupon) {
        if (template.getValidType() != null && template.getValidType() == 2
                && template.getValidDays() != null && userCoupon.getCreatedAt() != null) {
            return userCoupon.getCreatedAt().plusDays(template.getValidDays());
        }
        return template.getEndTime();
    }

    /**
     * 使用已查詢的範本資訊構建 UserCouponDTO
     */
    private UserCouponDTO buildUserCouponDTOWithTemplate(UserCoupon userCoupon, CouponTemplateDTO template) {
        UserCouponDTO dto = new UserCouponDTO();
        BeanUtils.copyProperties(userCoupon, dto);
        dto.setCouponName(template.getName());
        dto.setType(template.getType());
        dto.setThreshold(template.getThreshold());
        dto.setDiscount(template.getDiscount());
        dto.setExpireTime(template.getEndTime());
        return dto;
    }
}
