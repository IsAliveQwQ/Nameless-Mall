package com.nameless.mall.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.product.api.dto.ProductDTO;
import com.nameless.mall.product.api.dto.ProductDetailDTO;
import com.nameless.mall.product.entity.Product;

/**
 * 商品服務的接口
 * <p>
 * 繼承 IService<Product> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 * 此介面只包含與 Product 實體直接相關的業務邏輯。
 * Variant 相關邏輯請使用 {@link VariantService}。
 */
public interface ProductService extends IService<Product> {

    /**
     * 分頁查詢商品列表
     * 
     * @param pageNum    頁碼
     * @param pageSize   每頁數量
     * @param categoryId 分類ID（可選）
     * @return 分頁後的商品列表
     */
    Page<ProductDTO> getProductList(Integer pageNum, Integer pageSize, Long categoryId);

    /**
     * 根據商品 ID，查詢商品的完整詳細資訊。
     * <p>
     * 這個方法會聚合商品主表、分類表、規格表、選項表、圖片表等多張資料表的資訊，
     * 組合成一個單一的、可供前端商品詳細頁面直接使用的 ProductDetailDTO 物件。
     *
     * @param productId 商品的 ID
     * @return 商品的完整詳細資訊 DTO
     */
    ProductDetailDTO getProductDetailById(Long productId);

    /**
     * 取得商品詳情 (VO 版)，包含規格聚合、標籤、分類階層。
     * 詳情頁專用，不觸動舊端點。
     */
    com.nameless.mall.product.api.vo.ProductDetailVO getProductDetailVOById(Long productId);

    void createProduct(Product product);

    void modifyProduct(Product product);

    void deleteProduct(Long productId);
}
