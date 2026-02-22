package com.nameless.mall.promotion.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.promotion.api.dto.MarketingCampaignDTO;
import com.nameless.mall.promotion.api.vo.MarketingCampaignVO;
import com.nameless.mall.promotion.service.MarketingCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行銷活動 Controller。
 * 提供行銷活動查詢 API。
 */
@RestController
@RequestMapping("/promotions/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final MarketingCampaignService marketingCampaignService;

    /**
     * 獲取所有有效活動列表。
     */
    @GetMapping
    public Result<List<MarketingCampaignVO>> getCampaigns() {
        List<MarketingCampaignDTO> campaigns = marketingCampaignService.getActiveCampaigns();
        List<MarketingCampaignVO> vos = campaigns.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.ok(vos);
    }

    /**
     * 根據代碼獲取活動詳情。
     * 找不到活動會 throw NOT_FOUND exception。
     */
    @GetMapping("/{code}")
    public Result<MarketingCampaignVO> getByCode(@PathVariable String code) {
        MarketingCampaignDTO campaign = marketingCampaignService.getByCode(code);
        if (campaign == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return Result.ok(convertToVO(campaign));
    }

    private MarketingCampaignVO convertToVO(MarketingCampaignDTO dto) {
        MarketingCampaignVO vo = new MarketingCampaignVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }
}
