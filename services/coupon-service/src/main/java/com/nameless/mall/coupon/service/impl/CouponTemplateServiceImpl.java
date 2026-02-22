package com.nameless.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.coupon.api.dto.CouponTemplateDTO;
import com.nameless.mall.coupon.entity.CouponTemplate;
import com.nameless.mall.coupon.mapper.CouponTemplateMapper;
import com.nameless.mall.coupon.service.CouponTemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 優惠券範本服務實作
 * <p>
 * 負責管理優惠券範本的業務邏輯。
 */
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate>
        implements CouponTemplateService {

    // getCouponCards 已遷移至 CouponQueryService (CQRS 讀模型)

    @Override
    public List<CouponTemplateDTO> getAvailableTemplates() {
        // 1. 查詢所有啟用中、有餘量且在有效期內的優惠券範本
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = this.list(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, 1)
                        .gt(CouponTemplate::getRemainCount, 0)
                        .le(CouponTemplate::getStartTime, now)
                        .ge(CouponTemplate::getEndTime, now));

        // 2. 將實體轉換為 DTO 清單並回傳
        return templates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CouponTemplateDTO getTemplateById(Long templateId) {
        CouponTemplate template = this.getById(templateId);
        if (template == null) {
            return null;
        }
        return toDTO(template);
    }

    @Override
    public CouponTemplate validateAndGet(Long templateId) {
        // 1. 根據範本 ID 查詢優惠券範本
        CouponTemplate template = this.getById(templateId);

        // 2. 檢查範本是否存在且為啟用狀態
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_FOUND, "優惠券已下架或不存在");
        }

        // 3. 檢查剩餘可領取數量是否充足
        if (template.getRemainCount() <= 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_CLAIM_LIMIT_REACHED, "優惠券已被領完");
        }

        // 4. 驗證當前時間是否在優惠券有效期內
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getStartTime()) || now.isAfter(template.getEndTime())) {
            throw new BusinessException(ResultCodeEnum.COUPON_EXPIRED, "優惠券不在有效期內");
        }

        return template;
    }

    @Override
    public void decreaseRemainCount(Long templateId) {
        int affected = baseMapper.decreaseRemainCount(templateId);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_CLAIM_LIMIT_REACHED);
        }
    }

    @Override
    public List<CouponTemplateDTO> getTemplatesBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return this.listByIds(ids).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 將 Entity 轉換為 DTO
     */
    private CouponTemplateDTO toDTO(CouponTemplate template) {
        CouponTemplateDTO dto = new CouponTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }
}
