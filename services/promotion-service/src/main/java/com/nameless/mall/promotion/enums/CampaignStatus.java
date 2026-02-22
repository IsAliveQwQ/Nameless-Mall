package com.nameless.mall.promotion.enums;

import lombok.Getter;

/**
 * 行銷活動狀態枚舉。
 */
@Getter
public enum CampaignStatus {
    DRAFT("草稿"),
    UPCOMING("即將開始"),
    ONGOING("進行中"),
    ENDING_SOON("即將結束"),
    ENDED("已結束"),
    SUSPENDED("暫停");

    private final String description;

    CampaignStatus(String description) {
        this.description = description;
    }
}
