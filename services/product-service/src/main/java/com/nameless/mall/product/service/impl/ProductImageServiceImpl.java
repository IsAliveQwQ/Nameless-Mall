package com.nameless.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.product.entity.ProductImage;
import com.nameless.mall.product.mapper.ProductImageMapper;
import com.nameless.mall.product.service.ProductImageService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品圖片服務的實現類
 * <p>
 * 負責處理與商品圖片相關的業務邏輯，包括：
 * - 主圖查詢
 * - 圖片列表查詢
 */
@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage>
        implements ProductImageService {

    @Override
    public String getMainImageUrl(Long productId) {
        // 優先查找標記為主圖的圖片
        ProductImage mainImage = this.getOne(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .eq(ProductImage::getIsMain, true)
                        .last("LIMIT 1"));

        if (mainImage != null) {
            return mainImage.getUrl();
        }

        // 若無主圖，返回第一張圖片
        List<ProductImage> images = this.list(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .last("LIMIT 1"));

        if (!CollectionUtils.isEmpty(images)) {
            return images.get(0).getUrl();
        }

        return null;
    }

    @Override
    public Map<Long, String> getMainImageUrls(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }

        // 查詢所有涉及商品的圖片，按 sortOrder 排序
        List<ProductImage> allImages = this.list(
                new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSortOrder));

        if (CollectionUtils.isEmpty(allImages)) {
            return Collections.emptyMap();
        }

        // 按商品 ID 分組
        Map<Long, List<ProductImage>> groupMap = allImages.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        Map<Long, String> result = new HashMap<>();
        groupMap.forEach((pId, imgs) -> {
            // 優先找標記為 isMain 的
            ProductImage main = imgs.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getIsMain()))
                    .findFirst()
                    .orElse(imgs.get(0)); // 若無則取第一張
            result.put(pId, main.getUrl());
        });

        return result;
    }

    @Override
    public List<String> getImageUrlsByProductId(Long productId) {
        List<ProductImage> images = this.list(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .orderByAsc(ProductImage::getSortOrder));

        if (CollectionUtils.isEmpty(images)) {
            return List.of();
        }

        return images.stream()
                .map(ProductImage::getUrl)
                .collect(Collectors.toList());
    }
}
