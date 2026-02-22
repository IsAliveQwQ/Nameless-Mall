package com.nameless.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.product.entity.Variant;

import java.util.List;

/**
 * 商品規格庫存服務的接口
 * <p>
 * 繼承 IService<Variant> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 * 提供與 Variant 相關的業務邏輯，例如庫存管理、規格查詢等..。
 */
public interface VariantService extends IService<Variant> {

    /**
     * 獲取購物車所需的商品規格資訊
     * 
     * @param variantId 規格 ID
     * @return 規格詳情 DTO
     */
    VariantDTO getVariantForCart(Long variantId);

    /**
     * 批次扣減商品庫存
     * 
     * @param dtoList 扣減清單
     */
    void decreaseStock(List<DecreaseStockInputDTO> dtoList);

    /**
     * 批次返還商品庫存
     * 
     * @param dtoList 返還清單
     */
    void increaseStock(List<DecreaseStockInputDTO> dtoList);

    /**
     * 根據商品 ID 查詢所有規格
     * 
     * @param productId 商品 ID
     * @return 規格列表
     */
    List<VariantDTO> getVariantsByProductId(Long productId);

    /**
     * 批量查詢規格資訊 (Feign 專用)
     * 
     * @param ids SKU ID 列表
     * @return 規格DTO列表
     */
    List<VariantDTO> getVariantsByIds(List<Long> ids);

    /**
     * 批量根據商品ID查詢所有規格 (Feign 專用)
     */
    List<VariantDTO> getVariantsByProductIds(List<Long> productIds);
}
