package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 商品分類 VO - 樹狀結構
 */
@Data
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private List<CategoryVO> children;
}
