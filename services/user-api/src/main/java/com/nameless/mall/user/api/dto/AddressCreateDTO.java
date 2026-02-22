package com.nameless.mall.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

/**
 * 新增/更新收貨地址請求 DTO
 */
@Data
public class AddressCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "收件人姓名不能為空")
    @Size(max = 64, message = "收件人姓名長度不能超過 64 個字元")
    private String receiverName;

    @NotBlank(message = "收件人電話不能為空")
    @Size(max = 20, message = "收件人電話長度不能超過 20 個字元")
    private String receiverPhone;

    private String province;
    private String city;
    private String district;

    @NotBlank(message = "詳細地址不能為空")
    @Size(max = 255, message = "詳細地址長度不能超過 255 個字元")
    private String detailAddress;

    @Size(max = 10, message = "郵遞區號長度不能超過 10 個字元")
    private String postalCode;

    /** 是否設為預設地址 */
    private Boolean setDefault;

    @Size(max = 20, message = "標籤長度不能超過 20 個字元")
    private String tag;
}
