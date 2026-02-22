package com.nameless.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.product.api.dto.DecreaseStockInputDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.product.api.dto.VariantOptionDTO;
import com.nameless.mall.product.entity.Product;
import com.nameless.mall.product.entity.Variant;
import com.nameless.mall.product.entity.VariantOption;
import com.nameless.mall.product.mapper.ProductMapper;
import com.nameless.mall.product.mapper.VariantMapper;
import com.nameless.mall.product.mapper.VariantOptionMapper;
import com.nameless.mall.product.service.ProductImageService;
import com.nameless.mall.product.service.VariantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品規格庫存服務實作類
 * <p>
 * 負責處理與 Variant (SKU) 相關的業務邏輯，包括：
 * - 規格資訊查詢與聚合
 * - 庫存扣減與返還 (Atomic Update)
 */
@Service
@RequiredArgsConstructor
public class VariantServiceImpl extends ServiceImpl<VariantMapper, Variant> implements VariantService {

    private static final Logger log = LoggerFactory.getLogger(VariantServiceImpl.class);

    private final ProductMapper productMapper;
    private final VariantOptionMapper variantOptionMapper;
    private final ProductImageService productImageService;

    @Override
    public VariantDTO getVariantForCart(Long variantId) {
        Variant variant = this.getById(variantId);

        log.debug("【規格查詢】variantId={}, 結果={}", variantId, variant != null ? "找到" : "不存在");

        if (variant == null) {
            throw new BusinessException(ResultCodeEnum.VARIANT_NOT_FOUND,
                    "無法找到 ID 為 " + variantId + " 的商品規格，請確認資料正確性。");
        }

        Product product = productMapper.selectById(variant.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND,
                    "規格 ID " + variantId + " 關聯的商品主檔 (ID: " + variant.getProductId() + ") 已不存在");
        }

        // 透過 ProductImageService 取得主圖
        String mainImageUrl = productImageService.getMainImageUrl(product.getId());

        VariantDTO dto = new VariantDTO();
        BeanUtils.copyProperties(variant, dto);
        dto.setProductId(product.getId());
        dto.setCategoryId(product.getCategoryId());
        dto.setProductName(product.getName());
        // 圖片fallback：優先用規格圖，無圖則用商品主圖
        if (dto.getImage() == null || dto.getImage().isBlank()) {
            dto.setImage(mainImageUrl);
        }

        // 查詢規格選項
        List<VariantOption> variantOptions = variantOptionMapper.selectList(
                new LambdaQueryWrapper<VariantOption>().eq(VariantOption::getVariantId, variant.getId()));
        if (!CollectionUtils.isEmpty(variantOptions)) {
            List<VariantOptionDTO> optionDTOs = variantOptions.stream().map(option -> {
                VariantOptionDTO optionDTO = new VariantOptionDTO();
                BeanUtils.copyProperties(option, optionDTO);
                return optionDTO;
            }).collect(Collectors.toList());
            dto.setOptions(optionDTOs);
        }

        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void decreaseStock(List<DecreaseStockInputDTO> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return;
        }

        // 對資源進行排序，避免 deadlock (Circular Wait)
        // 確保所有線程都以相同的順序鎖定資源
        dtoList.sort((a, b) -> a.getVariantId().compareTo(b.getVariantId()));

        for (DecreaseStockInputDTO input : dtoList) {
            int affected = baseMapper.decreaseStock(input.getVariantId(), input.getQuantity());
            if (affected == 0) {
                Variant variant = this.getById(input.getVariantId());
                if (variant == null) {
                    throw new BusinessException(ResultCodeEnum.VARIANT_NOT_FOUND,
                            "庫存扣減失敗：找不到規格 " + input.getVariantId());
                }
                throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT, "商品 " + variant.getSku() + " 庫存不足");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void increaseStock(List<DecreaseStockInputDTO> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return;
        }

        for (DecreaseStockInputDTO input : dtoList) {
            baseMapper.increaseStock(input.getVariantId(), input.getQuantity());
        }
    }

    @Override
    public List<VariantDTO> getVariantsByProductId(Long productId) {
        List<Variant> variants = this.list(
                new LambdaQueryWrapper<Variant>().eq(Variant::getProductId, productId));

        if (CollectionUtils.isEmpty(variants)) {
            return List.of();
        }

        Product product = productMapper.selectById(productId);
        String productName = product != null ? product.getName() : null;
        String mainImageUrl = productImageService.getMainImageUrl(productId);

        // 批量查詢所有規格選項 (防止 N+1)
        List<Long> variantIds = variants.stream().map(Variant::getId).collect(Collectors.toList());
        List<VariantOption> allOptions = variantOptionMapper.selectList(
                new LambdaQueryWrapper<VariantOption>().in(VariantOption::getVariantId, variantIds));
        Map<Long, List<VariantOption>> optionsMap = allOptions.stream()
                .collect(Collectors.groupingBy(VariantOption::getVariantId));

        return variants.stream().map(variant -> {
            VariantDTO dto = new VariantDTO();
            BeanUtils.copyProperties(variant, dto);
            dto.setProductName(productName);
            // 圖像回退策略：優先用規格圖，無圖則用商品主圖
            if (dto.getImage() == null || dto.getImage().isBlank()) {
                dto.setImage(mainImageUrl);
            }

            // 從預載的 Map 取得規格選項
            List<VariantOption> options = optionsMap.getOrDefault(variant.getId(), List.of());
            if (!options.isEmpty()) {
                List<VariantOptionDTO> optionDTOs = options.stream().map(opt -> {
                    VariantOptionDTO optDto = new VariantOptionDTO();
                    BeanUtils.copyProperties(opt, optDto);
                    return optDto;
                }).collect(Collectors.toList());
                dto.setOptions(optionDTOs);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<VariantDTO> getVariantsByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        List<Variant> variants = this.listByIds(ids);
        if (CollectionUtils.isEmpty(variants)) {
            return List.of();
        }

        return convertToDTOs(variants);
    }

    @Override
    public List<VariantDTO> getVariantsByProductIds(List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return List.of();
        }

        List<Variant> variants = this.list(
                new LambdaQueryWrapper<Variant>().in(Variant::getProductId, productIds));

        if (CollectionUtils.isEmpty(variants)) {
            return List.of();
        }

        return convertToDTOs(variants);
    }

    // 提取公共轉換邏輯
    private List<VariantDTO> convertToDTOs(List<Variant> variants) {
        // 收集所有 ProductId 以批量查詢商品名稱與圖片
        List<Long> productIds = variants.stream().map(Variant::getProductId).distinct().collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (v1, v2) -> v1)); // mergeFunction防禦重複

        // 批量查詢主圖，避免迴圈內逐筆查詢 N+1
        Map<Long, String> mainImageMap = productImageService.getMainImageUrls(productIds);

        return variants.stream().map(variant -> {
            VariantDTO dto = new VariantDTO();
            BeanUtils.copyProperties(variant, dto);

            Product product = productMap.get(variant.getProductId());
            if (product != null) {
                dto.setCategoryId(product.getCategoryId());
                dto.setProductName(product.getName());
                String mainImageUrl = mainImageMap.get(product.getId());
                if (dto.getImage() == null || dto.getImage().isBlank()) {
                    dto.setImage(mainImageUrl);
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }
}
