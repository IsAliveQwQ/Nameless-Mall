package com.nameless.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.product.api.dto.ProductDTO;
import com.nameless.mall.product.api.dto.ProductDetailDTO;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.product.api.dto.VariantOptionDTO;
import com.nameless.mall.product.api.enums.ProductStatus;
import com.nameless.mall.product.api.vo.ProductDetailVO;
import com.nameless.mall.product.api.vo.VariantOptionVO;
import com.nameless.mall.product.api.vo.VariantVO;
import com.nameless.mall.product.entity.Category;
import com.nameless.mall.product.entity.Product;
import com.nameless.mall.product.entity.ProductTag;
import com.nameless.mall.product.entity.Tag;
import com.nameless.mall.product.event.ProductSyncEvent;
import com.nameless.mall.product.mapper.CategoryMapper;
import com.nameless.mall.product.mapper.ProductMapper;
import com.nameless.mall.product.mapper.ProductTagMapper;
import com.nameless.mall.product.service.CategoryService;
import com.nameless.mall.product.service.ProductImageService;
import com.nameless.mall.product.service.ProductService;
import com.nameless.mall.product.service.TagService;
import com.nameless.mall.product.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品服務實作類
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final ProductImageService productImageService;
    private final VariantService variantService;
    private final TagService tagService;
    private final ProductTagMapper productTagMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(Product product) {
        this.baseMapper.insert(product);
        publishSyncEvent(product.getId(), ACTION_UPDATE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product:detail:vo", key = "#product.id")
    public void modifyProduct(Product product) {
        this.baseMapper.updateById(product);
        publishSyncEvent(product.getId(), ACTION_UPDATE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product:detail:vo", key = "#productId")
    public void deleteProduct(Long productId) {
        this.baseMapper.deleteById(productId);
        publishSyncEvent(productId, ACTION_DELETE);
    }

    @Override
    public Page<ProductDTO> getProductList(Integer pageNum, Integer pageSize, Long categoryId) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductStatus.ON_SHELF.getCode());
        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }
        queryWrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> productPage = this.page(page, queryWrapper);
        List<Product> records = productPage.getRecords();

        if (CollectionUtils.isEmpty(records)) {
            return new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        }

        List<Long> productIds = records.stream().map(Product::getId).collect(Collectors.toList());

        Map<Long, String> categoryMap = fetchCategoryNames(records);
        Map<Long, List<String>> tagsMap = fetchTagsMap(productIds);
        Map<Long, String> mainImageMap = productImageService.getMainImageUrls(productIds);

        List<ProductDTO> dtos = records.stream().map(product -> {
            ProductDTO dto = new ProductDTO();
            BeanUtils.copyProperties(product, dto);
            dto.setCategoryName(categoryMap.get(product.getCategoryId()));
            dto.setTags(tagsMap.getOrDefault(product.getId(), Collections.emptyList()));
            dto.setMainImage(mainImageMap.get(product.getId()));
            return dto;
        }).collect(Collectors.toList());

        Page<ProductDTO> resultPage = new Page<>(productPage.getCurrent(), productPage.getSize(),
                productPage.getTotal());
        resultPage.setRecords(dtos);
        return resultPage;
    }

    @Override
    public ProductDetailDTO getProductDetailById(Long productId) {
        Product product = fetchAndValidateProduct(productId);
        ProductDetailDTO dto = new ProductDetailDTO();
        BeanUtils.copyProperties(product, dto);
        enrichProductDetail(dto, product);
        return dto;
    }

    @Cacheable(value = "product:detail:vo", key = "#productId")
    @Override
    public ProductDetailVO getProductDetailVOById(Long productId) {
        Product product = fetchAndValidateProduct(productId);
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);

        vo.setCategoryName(getCategoryName(product.getCategoryId()));
        vo.setTags(getTagNamesByProductId(product.getId()));
        vo.setImages(productImageService.getImageUrlsByProductId(product.getId()));
        vo.setMainImage(productImageService.getMainImageUrl(product.getId()));
        vo.setCategoryHierarchy(categoryService.getCategoryPath(product.getCategoryId()));

        List<VariantDTO> variantDTOs = variantService.getVariantsByProductId(product.getId());
        List<VariantVO> variantVOs = variantDTOs.stream().map(this::convertToVariantVO).collect(Collectors.toList());
        vo.setVariants(variantVOs);
        vo.setDisplayOptions(aggregateDisplayOptions(variantVOs));
        vo.setStock(variantVOs.stream().mapToInt(VariantVO::getStock).sum());

        return vo;
    }

    private Product fetchAndValidateProduct(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND, "找不到指定的商品或該商品已下架");
        }
        return product;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null)
            return null;
        Category category = categoryMapper.selectById(categoryId);
        return category != null ? category.getName() : null;
    }

    private void enrichProductDetail(ProductDetailDTO dto, Product product) {
        dto.setCategoryName(getCategoryName(product.getCategoryId()));
        dto.setImages(productImageService.getImageUrlsByProductId(product.getId()));
        List<VariantDTO> variants = variantService.getVariantsByProductId(product.getId());
        dto.setVariants(variants);
        dto.setTags(getTagNamesByProductId(product.getId()));
        if (!CollectionUtils.isEmpty(variants)) {
            dto.setDisplayOptions(aggregateDisplayOptionsFromDTO(variants));
        }
    }

    private Map<String, List<String>> aggregateDisplayOptionsFromDTO(List<VariantDTO> variants) {
        Map<String, Set<String>> temp = new LinkedHashMap<>();
        for (VariantDTO v : variants) {
            if (v.getOptions() != null) {
                for (VariantOptionDTO opt : v.getOptions()) {
                    temp.computeIfAbsent(opt.getOptionName(), k -> new LinkedHashSet<>()).add(opt.getOptionValue());
                }
            }
        }
        Map<String, List<String>> res = new LinkedHashMap<>();
        temp.forEach((k, v) -> res.put(k, new ArrayList<>(v)));
        return res;
    }

    private Map<Long, String> fetchCategoryNames(List<Product> products) {
        Set<Long> cids = products.stream().map(Product::getCategoryId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (cids.isEmpty())
            return Collections.emptyMap();
        return categoryMapper.selectBatchIds(cids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private Map<Long, List<String>> fetchTagsMap(List<Long> productIds) {
        List<ProductTag> relations = productTagMapper
                .selectList(new LambdaQueryWrapper<ProductTag>().in(ProductTag::getProductId, productIds));
        if (CollectionUtils.isEmpty(relations))
            return Collections.emptyMap();
        Set<Long> tagIds = relations.stream().map(ProductTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameMap = tagService.listByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        Map<Long, List<String>> result = new HashMap<>();
        for (ProductTag pt : relations) {
            String name = tagNameMap.get(pt.getTagId());
            if (name != null) {
                result.computeIfAbsent(pt.getProductId(), k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    private List<String> getTagNamesByProductId(Long productId) {
        List<ProductTag> relations = productTagMapper
                .selectList(new LambdaQueryWrapper<ProductTag>().eq(ProductTag::getProductId, productId));
        if (relations.isEmpty())
            return Collections.emptyList();
        return tagService.getTagNamesByIds(relations.stream().map(ProductTag::getTagId).collect(Collectors.toList()));
    }

    private VariantVO convertToVariantVO(VariantDTO dto) {
        VariantVO vo = new VariantVO();
        BeanUtils.copyProperties(dto, vo);
        if (dto.getOptions() != null) {
            vo.setOptions(dto.getOptions().stream().map(o -> {
                VariantOptionVO ovo = new VariantOptionVO();
                BeanUtils.copyProperties(o, ovo);
                return ovo;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    private Map<String, List<String>> aggregateDisplayOptions(List<VariantVO> variants) {
        Map<String, Set<String>> temp = new LinkedHashMap<>();
        for (VariantVO v : variants) {
            if (v.getOptions() != null) {
                for (VariantOptionVO opt : v.getOptions()) {
                    temp.computeIfAbsent(opt.getOptionName(), k -> new LinkedHashSet<>()).add(opt.getOptionValue());
                }
            }
        }
        Map<String, List<String>> res = new LinkedHashMap<>();
        temp.forEach((k, v) -> res.put(k, new ArrayList<>(v)));
        return res;
    }

    private void publishSyncEvent(Long productId, String action) {
        if (productId != null) {
            log.debug("【商品服務】發送內部同步事件: ID={}, Action={}", productId, action);
            eventPublisher.publishEvent(new ProductSyncEvent(this, productId, action));
        }
    }
}