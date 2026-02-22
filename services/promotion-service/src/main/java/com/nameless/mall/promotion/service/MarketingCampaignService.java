package com.nameless.mall.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.promotion.api.dto.MarketingCampaignDTO;
import com.nameless.mall.promotion.entity.MarketingCampaign;

import java.util.List;

/**
 * 行銷活動服務介面。
 * 負責行銷活動的查詢與狀態管理。
 */
public interface MarketingCampaignService extends IService<MarketingCampaign> {

    /**
     * 獲取所有展示中的活動（按權重排序）。
     */
    List<MarketingCampaignDTO> getActiveCampaigns();

    /**
     * 根據代碼獲取活動詳情。
     */
    MarketingCampaignDTO getByCode(String code);
}
