package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 篩選器選項 VO
 * <p>
 * 用於前端渲染單個篩選選項，例如：
 * - 品牌: { id:1, name:"HAY", count:12 }
 * - 顏色: { value:"紅色", count:5 }
 */
@Data
public class FilterItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 選項 ID (用於分類或品牌篩選，屬性篩選可為空)
     */
    private Long id;

    /**
     * 顯示名稱
     */
    private String name;

    /**
     * 查詢值 (搜尋時透過此值回傳後端)
     */
    private String value;

    /**
     * 該選項下的商品數量 (ES聚合結果)
     */
    private Long count;

    /**
     * 圖片 URL (用於品牌牆或圖像化篩選)
     */
    private String image;
}
