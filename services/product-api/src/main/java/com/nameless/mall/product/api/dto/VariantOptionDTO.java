package com.nameless.mall.product.api.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 商品規格選項資料傳輸物件
 * <p>
 * 用於表示一個具體的規格鍵值對，例如 (顏色: 紅色) 或 (尺寸: L)。
 */
@Data
public class VariantOptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 選項類型 (例如 "顏色", "尺寸", "容量")
     */
    private String optionName;

    /**
     * 選項值 (例如 "紅色", "L", "256GB")
     */
    private String optionValue;
}
