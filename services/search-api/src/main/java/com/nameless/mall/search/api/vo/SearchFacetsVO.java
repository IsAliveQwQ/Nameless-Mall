package com.nameless.mall.search.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 搜尋聚合面板 VO (Facets/Filters)
 * 包含所有可供篩選的維度
 */
@Data
public class SearchFacetsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分類聚合列表
     */
    private List<FacetItemVO> categories;

    /**
     * 標籤聚合列表
     */
    private List<FacetItemVO> tags;

    /**
     * 價格區間統計
     */
    private PriceStats priceStatistics;

    /**
     * 動態規格屬性聚合 (e.g. 顏色: [紅, 黑], 材質: [木, 冷軋鋼])
     */
    private List<AttributeFacet> attributes;

    @Data
    public static class AttributeFacet implements Serializable {
        private Long attrId;
        private String attrName;
        private List<AttrValueVO> attrValues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrValueVO implements Serializable {
        private String value;
        private Long count;
    }

    @Data
    public static class PriceStats implements Serializable {
        private BigDecimal min;
        private BigDecimal max;
        private Long count;
    }
}
