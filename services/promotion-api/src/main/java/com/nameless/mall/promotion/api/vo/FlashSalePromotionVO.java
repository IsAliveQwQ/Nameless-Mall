package com.nameless.mall.promotion.api.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 限時特賣活動 VO
 */
@Data
public class FlashSalePromotionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String bannerImage;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /** 狀態: 0=未開始 1=進行中 2=已結束 */
    private Integer status;
}
