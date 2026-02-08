package com.nameless.mall.product.api.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 屬性篩選區塊 VO
 * <p>
 * 用於渲染一個完整的屬性篩選群組，例如 "材質" 區塊。
 */
@Data
public class AttributeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 屬性 ID (對應後端 attrId)
     */
    private Long attrId;

    /**
     * 屬性名稱 (如 "材質", "顏色")
     */
    private String attrName;

    /**
     * 該屬性下的所有可選值
     */
    private List<FilterItemVO> attrValues;
}
