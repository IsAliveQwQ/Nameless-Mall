package com.nameless.mall.promotion.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒殺活動場次 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlashSaleSessionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String statusText;
    private String bannerImage;
    private Long countdownSeconds;

    private List<FlashSaleProductVO> products;
}
