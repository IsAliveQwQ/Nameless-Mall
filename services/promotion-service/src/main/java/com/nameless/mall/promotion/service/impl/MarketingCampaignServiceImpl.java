package com.nameless.mall.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.promotion.api.dto.MarketingCampaignDTO;
import com.nameless.mall.promotion.entity.MarketingCampaign;
import com.nameless.mall.promotion.enums.CampaignStatus;
import com.nameless.mall.promotion.mapper.MarketingCampaignMapper;
import com.nameless.mall.promotion.service.MarketingCampaignService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行銷活動服務實現。
 * 負責活動狀態的動態計算與查詢。
 */
@Service
public class MarketingCampaignServiceImpl extends ServiceImpl<MarketingCampaignMapper, MarketingCampaign>
        implements MarketingCampaignService {

    /**
     * 獲取所有有效活動，並動態判定其狀態。
     * 只會回傳非草稿、非停用的活動。
     */
    @Override
    public List<MarketingCampaignDTO> getActiveCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        // 1. 查詢非草稿、非停用且未過期的活動
        List<MarketingCampaign> campaigns = this.list(new LambdaQueryWrapper<MarketingCampaign>()
                .notIn(MarketingCampaign::getStatus, CampaignStatus.DRAFT.name(), CampaignStatus.SUSPENDED.name())
                .ge(MarketingCampaign::getEndTime, now.minusDays(1)) // 只載入尚未結束或剛結束的活動
                .orderByDesc(MarketingCampaign::getDisplayOrder)
                .orderByDesc(MarketingCampaign::getId));

        // 2. 動態計算每個活動的即時狀態並轉為 DTO
        return campaigns.stream().map(c -> {
            c.setStatus(computeStatus(c, now));
            return toDTO(c);
        }).collect(Collectors.toList());
    }

    /**
     * 根據當前時間與活動起訖時間判定狀態。
     */
    private String computeStatus(MarketingCampaign c, LocalDateTime now) {
        // 1. 缺少時間欄位時保留原始狀態
        if (c.getStartTime() == null || c.getEndTime() == null)
            return c.getStatus();

        // 2. 根據當前時間判定活動所處階段
        if (now.isBefore(c.getStartTime()))
            return CampaignStatus.UPCOMING.name();
        if (now.isAfter(c.getEndTime()))
            return CampaignStatus.ENDED.name();

        // 3. 倒數 24 小時內標記為即將結束，否則為進行中
        return c.getEndTime().minusHours(24).isBefore(now)
                ? CampaignStatus.ENDING_SOON.name()
                : CampaignStatus.ONGOING.name();
    }

    @Override
    public MarketingCampaignDTO getByCode(String code) {
        MarketingCampaign entity = this
                .getOne(new LambdaQueryWrapper<MarketingCampaign>().eq(MarketingCampaign::getCode, code));
        return entity == null ? null : toDTO(entity);
    }

    private MarketingCampaignDTO toDTO(MarketingCampaign entity) {
        MarketingCampaignDTO dto = new MarketingCampaignDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
