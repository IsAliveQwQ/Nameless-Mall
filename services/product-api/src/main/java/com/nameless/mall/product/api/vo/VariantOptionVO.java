package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 規格選項 VO
 * <p>
 * 用於 VariantVO 內部，描述該 SKU 的具體屬性組合。
 * 例如：{ "optionName": "顏色", "optionValue": "紅色" }
 */
@Data
public class VariantOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String optionName;
    private String optionValue;
}
