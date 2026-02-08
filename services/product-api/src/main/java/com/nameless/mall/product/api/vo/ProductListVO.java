package com.nameless.mall.product.api.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表 VO - 用於商品列表或搜尋結果展示
 */
@Data
public class ProductListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String title;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Long categoryId;
    private String categoryName;
    private String mainImage;
    private List<String> tags;
    private Integer sales;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
}
