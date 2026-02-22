package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 規格變體 VO (Variant View Object)
 * <p>
 * 代表一個具體的 SKU，包含其庫存、價格以及組成的選項。
 */
@Data
public class VariantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * SKU 編碼
     */
    private String sku;

    /**
     * 規格名稱 (如 "黑色 128G")
     */
    private String name;

    private BigDecimal price;

    /**
     * 此規格的原價
     */
    private BigDecimal originalPrice;

    private Integer stock;

    private String image;

    /**
     * 組成此規格的選項列表
     */
    private List<VariantOptionVO> options;
}
