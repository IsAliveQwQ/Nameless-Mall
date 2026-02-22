package com.nameless.mall.search.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品搜尋結果 DTO
 */
@Data
public class ProductSearchResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private BigDecimal price;
    private String mainImage;
    private String categoryName;
    /** 銷量 */
    private Integer sales;
}

