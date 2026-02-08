package com.nameless.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.product.entity.ProductImage;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商品圖片服務的接口
 * <p>
 * 繼承 IService<ProductImage> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 * 提供與商品圖片相關的業務邏輯。
 */
public interface ProductImageService extends IService<ProductImage> {

    /**
     * 獲取商品的主圖 URL
     * 
     * @param productId 商品 ID
     * @return 主圖 URL，若無則返回 null
     */
    String getMainImageUrl(Long productId);

    /**
     * 批量獲取商品的主圖 URL
     * 
     * @param productIds 商品 ID 列表
     * @return Map<productId, mainImageUrl>
     */
    Map<Long, String> getMainImageUrls(Collection<Long> productIds);

    /**
     * 獲取商品的所有圖片 URL
     * 
     * @param productId 商品 ID
     * @return 圖片 URL 列表
     */
    List<String> getImageUrlsByProductId(Long productId);
}
