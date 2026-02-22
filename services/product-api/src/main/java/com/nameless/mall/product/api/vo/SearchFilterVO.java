package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 搜尋結果 - 篩選面板
 * <p>
 * 包含所有可用的動態篩選條件。
 */
@Data
public class SearchFilterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分類篩選列表 (展示當前結果分布的分類)
     */
    private List<FilterItemVO> categories;

    /**
     * 品牌篩選列表
     */
    private List<FilterItemVO> brands;

    /**
     * 屬性篩選列表 (顏色、材質等)
     */
    private List<AttributeVO> attributes;

}
