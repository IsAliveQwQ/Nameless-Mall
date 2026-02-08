package com.nameless.mall.search.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 搜尋請求物件
 */
@Data
public class SearchRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 關鍵字
     */
    private String keyword;

    /**
     * 分類 ID 篩選
     */
    private Long categoryId;

    /**
     * 標籤篩選
     */
    private String tag;

    /**
     * 最低價格
     */
    private BigDecimal minPrice;

    /**
     * 最高價格
     */
    private BigDecimal maxPrice;

    /**
     * 排序方式
     * price_asc: 價格由低到高
     * price_desc: 價格由高到低
     * sales_desc: 銷量優先
     * newest: 最新上架
     */
    private String sort;

    /**
     * 頁碼 (從 1 開始)
     */
    private Integer pageNum = 1;

    /**
     * 每頁筆數
     */
    private Integer pageSize = 20;

    /**
     * 屬性過濾 (格式: key:val1,val2;key:val)
     */
    private String attrs;
}
