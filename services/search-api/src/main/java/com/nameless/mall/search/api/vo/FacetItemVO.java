package com.nameless.mall.search.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 聚合篩選項 VO
 * 例如: { "name": "手機", "id": 1, "count": 120 }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 項目標籤 (名稱)
     */
    private String name;

    /**
     * 項目唯一標識 (如分類 ID)，標籤聚合時可能為空
     */
    private Object id;

    /**
     * 命中筆數
     */
    private Long count;
}
