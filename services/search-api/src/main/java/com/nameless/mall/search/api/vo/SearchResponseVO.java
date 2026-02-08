package com.nameless.mall.search.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 搜尋回應對象，包含商品分頁資料與篩選側邊欄聚合結果。
 */
@Data
public class SearchResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品列表
     */
    private List<ProductSearchVO> products;

    /**
     * 總命中筆數
     */
    private Long total;

    /**
     * 總頁數
     */
    private Integer totalPages;

    /**
     * 當前頁碼
     */
    private Integer pageNum;

    /**
     * 聚合統計資訊 (側邊欄)
     */
    private SearchFacetsVO facets;
}
