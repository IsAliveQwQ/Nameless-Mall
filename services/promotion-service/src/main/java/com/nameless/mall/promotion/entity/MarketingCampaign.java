package com.nameless.mall.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行銷活動實體類
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("marketing_campaigns")
public class MarketingCampaign implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String period;

    private String code;

    private String status;

    private String imageUrl;

    private Integer displayOrder;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long categoryId;

    private BigDecimal discountRate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
