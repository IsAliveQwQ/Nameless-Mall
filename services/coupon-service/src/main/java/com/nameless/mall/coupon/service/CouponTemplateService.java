package com.nameless.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.coupon.api.dto.CouponTemplateDTO;
import com.nameless.mall.coupon.entity.CouponTemplate;

import java.util.List;

/**
 * 優惠券範本服務介面
 * <p>
 * 負責管理優惠券範本的業務邏輯，包括查詢、庫存扣減等。
 */
public interface CouponTemplateService extends IService<CouponTemplate> {

    /**
     * 獲取可領取的優惠券範本列表
     * 
     * @return 符合條件的範本 DTO 列表
     */
    List<CouponTemplateDTO> getAvailableTemplates();

    // getCouponCards 已遷移至 CouponQueryService (CQRS 讀模型)

    /**
     * 根據 ID 獲取範本資訊
     * 
     * @param templateId 範本 ID
     * @return 範本 DTO，若不存在返回 null
     */
    CouponTemplateDTO getTemplateById(Long templateId);

    /**
     * 檢查範本是否可被領取
     * 
     * @param templateId 範本 ID
     * @return 範本實體
     * @throws com.nameless.mall.core.exception.BusinessException 若不可領取
     */
    CouponTemplate validateAndGet(Long templateId);

    /**
     * 原子操作扣減剩餘數量
     * 
     * @param templateId 範本 ID
     * @throws com.nameless.mall.core.exception.BusinessException 若庫存不足
     */
    void decreaseRemainCount(Long templateId);

    /**
     * 批量獲取範本資訊
     */
    List<CouponTemplateDTO> getTemplatesBatch(List<Long> ids);
}
