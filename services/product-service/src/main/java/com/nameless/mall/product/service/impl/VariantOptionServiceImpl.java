package com.nameless.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.product.entity.VariantOption;
import com.nameless.mall.product.mapper.VariantOptionMapper;
import com.nameless.mall.product.service.VariantOptionService;
import org.springframework.stereotype.Service;

/**
 * 商品規格選項值服務的實現類
 */
@Service
public class VariantOptionServiceImpl extends ServiceImpl<VariantOptionMapper, VariantOption> implements VariantOptionService {
    // 目前暫時不需要自訂的業務邏輯，
    // ServiceImpl 已經提供了足夠的基礎功能。
}
