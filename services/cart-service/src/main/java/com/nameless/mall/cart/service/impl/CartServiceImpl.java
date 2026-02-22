package com.nameless.mall.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nameless.mall.cart.api.dto.CartDTO;
import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.cart.service.CartService;
import com.nameless.mall.cart.api.vo.CartItemVO;
import com.nameless.mall.cart.api.vo.CartVO;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.product.api.dto.VariantDTO;
import com.nameless.mall.product.api.dto.VariantOptionDTO;
import com.nameless.mall.product.api.feign.ProductFeignClient;
import com.nameless.mall.promotion.api.dto.ProductPriceCheckDTO;
import com.nameless.mall.promotion.api.dto.ProductPriceResultDTO;
import com.nameless.mall.promotion.api.enums.PromotionType;
import com.nameless.mall.promotion.api.feign.PromotionFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 購物車服務實作，以 Redis Hash 儲存購物車資料。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String CART_PREFIX = "mall:cart:";
    private static final String DEFAULT_SPEC = "預設規格";

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final ProductFeignClient productFeignClient;
    private final PromotionFeignClient promotionFeignClient;
    private final ObjectMapper objectMapper;
    private final Executor cartFeignExecutor;

    /**
     * 加入商品至購物車邏輯
     * 1. 檢查購物車中是否已存在該規格項目
     * 2. 若存在則累加數量
     * 3. 若不存在則透過 Feign 獲取商品規格詳情並初始化項目
     * 
     * @param variantId 商品規格 ID
     * @param quantity  加入數量
     * @return 更新後的購物車對象
     */
    @Override
    public CartVO addToCart(Long userId, Long variantId, Integer quantity) {
        log.info("【購物車服務】處理加入商品請求: userId={}, variantId={}, quantity={}", userId, variantId, quantity);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps(userId);

        // Phase 1: 鎖外預取 — 冪等讀取，不需要互斥保護
        String existingJson = (String) cartOps.get(variantId.toString());
        final VariantDTO prefetchedVariant;
        if (!StringUtils.hasText(existingJson)) {
            Result<VariantDTO> variantResult = productFeignClient.getVariantById(variantId);
            if (variantResult == null || !variantResult.isSuccess() || variantResult.getData() == null) {
                throw new BusinessException(ResultCodeEnum.VARIANT_NOT_FOUND);
            }
            prefetchedVariant = variantResult.getData();
            if (prefetchedVariant.getStock() == null || prefetchedVariant.getStock() < quantity) {
                throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT);
            }
        } else {
            prefetchedVariant = null;
        }

        // Phase 2: 鎖內原子寫入 — 僅保護 Redis 寫操作 (< 50ms)
        executeWithLock(userId, () -> {
            try {
                // Double-Check: 鎖內重新讀取，防止並發窗口期的重複新增
                String json = (String) cartOps.get(variantId.toString());
                if (StringUtils.hasText(json)) {
                    CartItemDTO cartItem = objectMapper.readValue(json, CartItemDTO.class);
                    cartItem.setQuantity(cartItem.getQuantity() + quantity);
                    cartOps.put(variantId.toString(), objectMapper.writeValueAsString(cartItem));
                } else if (prefetchedVariant != null) {
                    // originalPrice 必須是 MSRP（定價），而非當前售價
                    BigDecimal msrp = prefetchedVariant.getOriginalPrice() != null
                            ? prefetchedVariant.getOriginalPrice()
                            : prefetchedVariant.getPrice();
                    CartItemDTO cartItem = CartItemDTO.builder()
                            .productId(prefetchedVariant.getProductId())
                            .categoryId(prefetchedVariant.getCategoryId())
                            .productName(prefetchedVariant.getProductName())
                            .productImage(prefetchedVariant.getImage())
                            .variantId(variantId)
                            .sku(prefetchedVariant.getSku())
                            .options(
                                    prefetchedVariant.getOptions() != null ? prefetchedVariant.getOptions() : List.of())
                            .quantity(quantity)
                            .price(prefetchedVariant.getPrice())
                            .originalPrice(msrp)
                            .discountAmount(BigDecimal.ZERO)
                            .promotionType(PromotionType.NONE.name())
                            .build();
                    cartOps.put(variantId.toString(), objectMapper.writeValueAsString(cartItem));
                }
            } catch (JsonProcessingException e) {
                log.error("【購物車錯誤】數據序列化失敗: variantId={}", variantId, e);
                throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "購物車數據處理失敗");
            }
            return null;
        });

        // Phase 3: 鎖外快速回傳 — 僅讀 Redis，不觸發 Feign 計價
        // 前端 mutation onSuccess 會 invalidateQueries 觸發 GET /cart 取得完整促銷價格
        return getCartQuick(userId);
    }

    /**
     * 獲取當前用戶的購物車 (VO) — 包含完整促銷計價（會呼叫 Feign）。
     */
    @Override
    public CartVO getCart(Long userId) {
        return toVO(getCartDTO(userId));
    }

    /**
     * 快速獲取購物車 — 僅從 Redis 讀取，不觸發任何 Feign 遠端呼叫。
     * <p>
     * 用於寫入操作 (addToCart / updateQuantity / removeItem) 的回傳值。
     * 使用 Redis 中已快取的價格，避免每次寫入串行呼叫 product-service + promotion-service。
     * 前端收到後會透過 invalidateQueries 自動 re-fetch GET /cart 取得完整促銷計價。
     */
    private CartVO getCartQuick(Long userId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps(userId);
        List<CartItemDTO> items = parseItemsFromRedis(cartOps);
        if (items.isEmpty()) {
            return toVO(buildEmptyCart());
        }
        return toVO(assembleFinalCart(items));
    }

    /**
     * 獲取當前用戶的購物車 (DTO)，包含促銷計算與降級邏輯。
     * 
     * 流程：Redis 讀取 → 資料完整性修復 → 價格計算（含降級）→ 總計聚合
     */
    @Override
    public CartDTO getCartDTO(Long userId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps(userId);

        // 1. 從 Redis 解析購物車項目
        List<CartItemDTO> items = parseItemsFromRedis(cartOps);
        if (items.isEmpty()) {
            return buildEmptyCart();
        }

        // 2+3. 並行發射：資料完整性修復 + 價格計算（兩者修改不同欄位，互不衝突）
        CompletableFuture<Void> categoryFuture = CompletableFuture.runAsync(
                () -> ensureCategoryIdIntegrity(items, cartOps), cartFeignExecutor);
        CompletableFuture<Void> pricingFuture = CompletableFuture.runAsync(
                () -> enrichWithPricing(items), cartFeignExecutor);

        try {
            CompletableFuture.allOf(categoryFuture, pricingFuture).join();
        } catch (Exception e) {
            log.error("【購物車服務】並行 Feign 呼叫異常，降級為串行重試", e);
            // 降級：若並行失敗，重新串行執行確保功能不中斷
            ensureCategoryIdIntegrity(items, cartOps);
            enrichWithPricing(items);
        }

        // 4. 聚合總計並返回
        return assembleFinalCart(items);
    }

    /**
     * 從 Redis 解析購物車項目列表。
     */
    private List<CartItemDTO> parseItemsFromRedis(BoundHashOperations<String, Object, Object> cartOps) {
        List<Object> cartItemJsonList = cartOps.values();

        if (CollectionUtils.isEmpty(cartItemJsonList)) {
            log.debug("【購物車服務】購物車目前為空");
            return List.of();
        }

        return cartItemJsonList.stream().map(json -> {
            try {
                return objectMapper.readValue((String) json, CartItemDTO.class);
            } catch (JsonProcessingException e) {
                log.warn("【購物車診斷】購物車項目 JSON 反序列化失敗，該項目將被忽略: json={}", json, e);
                return null;
            }
        }).filter(item -> item != null).collect(Collectors.toList());
    }

    /**
     * 建構空購物車 DTO。
     */
    private CartDTO buildEmptyCart() {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setItems(List.of());
        cartDTO.setTotalQuantity(0);
        cartDTO.setTotalPrice(BigDecimal.ZERO);
        return cartDTO;
    }

    /**
     * 資料完整性修復：補全缺少 categoryId 的項目。
     * 
     * 對於舊版購物車項目可能缺少 categoryId，需透過 Feign 補全並回寫 Redis。
     */
    private void ensureCategoryIdIntegrity(List<CartItemDTO> items,
            BoundHashOperations<String, Object, Object> cartOps) {
        // 1. 篩選出缺少 categoryId 的購物車項目
        List<CartItemDTO> missingCategoryItems = items.stream()
                .filter(i -> i.getCategoryId() == null)
                .collect(Collectors.toList());

        if (missingCategoryItems.isEmpty()) {
            return;
        }

        log.info("【購物車診斷】修復 {} 筆缺少 categoryId 的項目", missingCategoryItems.size());

        try {
            // 2. 收集需修復項目的 variantId 清單
            List<Long> variantIds = missingCategoryItems.stream()
                    .map(CartItemDTO::getVariantId)
                    .collect(Collectors.toList());

            // 3. 透過 Feign 批量查詢商品規格以取得 categoryId
            Result<List<VariantDTO>> batchResult = productFeignClient.getVariantsBatch(variantIds);
            if (batchResult != null && batchResult.isSuccess() && batchResult.getData() != null) {
                // 4. 建立對照表並逐一回寫修復後的 categoryId 至 Redis
                Map<Long, VariantDTO> variantMap = batchResult.getData().stream()
                        .collect(Collectors.toMap(VariantDTO::getId, v -> v, (a, b) -> a));

                for (CartItemDTO item : missingCategoryItems) {
                    VariantDTO variant = variantMap.get(item.getVariantId());
                    if (variant != null && variant.getCategoryId() != null) {
                        item.setCategoryId(variant.getCategoryId());
                        cartOps.put(item.getVariantId().toString(), objectMapper.writeValueAsString(item));
                    }
                }
            }
        } catch (Exception e) {
            log.error("【購物車診斷】批量補全 categoryId 失敗", e);
        }
    }

    /**
     * 價格計算：調用促銷服務計算最佳價格，含服務降級邏輯。
     */
    private void enrichWithPricing(List<CartItemDTO> items) {
        // 1. 將購物車項目轉換為促銷服務的價格查詢 DTO 清單
        List<ProductPriceCheckDTO> checkList = items.stream().map(item -> ProductPriceCheckDTO.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .categoryId(item.getCategoryId())
                .originalPrice(item.getOriginalPrice())
                .build()).collect(Collectors.toList());

        // 2. 呼叫促銷服務批量計算每個商品的最佳價格
        Result<List<ProductPriceResultDTO>> priceResults = promotionFeignClient.calculateBestPrices(checkList);

        // 3. 根據計價結果更新每個項目的售價、折扣金額與促銷資訊
        if (priceResults != null && priceResults.isSuccess() && priceResults.getData() != null) {
            Map<Long, ProductPriceResultDTO> priceMap = priceResults.getData().stream()
                    .collect(Collectors.toMap(ProductPriceResultDTO::getVariantId, p -> p, (a, b) -> a));

            for (CartItemDTO item : items) {
                ProductPriceResultDTO best = priceMap.get(item.getVariantId());
                if (best != null && best.getFinalPrice() != null) {
                    // 用計價引擎回傳的 MSRP 校正 originalPrice，確保折扣基準正確
                    if (best.getOriginalPrice() != null) {
                        item.setOriginalPrice(best.getOriginalPrice());
                    }
                    item.setPrice(best.getFinalPrice());
                    item.setDiscountAmount(item.getOriginalPrice().subtract(best.getFinalPrice()));
                    item.setPromotionName(best.getPromotionName());
                    item.setPromotionType(best.getPromotionType());
                    item.setPromotionId(best.getPromotionId());
                } else {
                    item.setPrice(item.getOriginalPrice());
                    item.setDiscountAmount(BigDecimal.ZERO);
                    item.setPromotionName(null);
                    item.setPromotionType(null);
                }
            }
        } else {
            // 4. [降級策略] 促銷服務不可用時保留 Redis 既有價格，僅標記狀態防止價格劇烈跳變
            log.warn("【購物車計價】促銷服務不可用，啟動平滑降級");
            for (CartItemDTO item : items) {
                item.setPromotionType(PromotionType.DEGRADED.name());
                // 若原本無價格，才補回原價
                if (item.getPrice() == null) {
                    item.setPrice(item.getOriginalPrice());
                    item.setDiscountAmount(BigDecimal.ZERO);
                }
            }
        }
    }

    /**
     * 聚合購物車總計並組裝最終 DTO。
     */
    private CartDTO assembleFinalCart(List<CartItemDTO> items) {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setItems(items);

        // 聚合總計
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartItemDTO item : items) {
            if (item.getPrice() != null && item.getQuantity() != null) {
                // 每筆小計皆應標準化精度，防止複利累加偏差
                BigDecimal itemSubtotal = item.getPrice()
                        .multiply(new BigDecimal(item.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                totalPrice = totalPrice.add(itemSubtotal);
                totalQuantity += item.getQuantity();
            }
        }

        cartDTO.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));
        cartDTO.setTotalQuantity(totalQuantity);

        return cartDTO;
    }

    /**
     * 更新購物車商品數量
     * 
     * @param variantId 商品規格 ID
     * @param quantity  欲更新的數量 (<=0 則移除)
     * @return 更新後的購物車內容 (VO)
     */
    @Override
    public CartVO updateItemQuantity(Long userId, Long variantId, Integer quantity) {
        if (quantity <= 0)
            return removeItem(userId, variantId);

        // 鎖內僅做 Redis 寫入
        executeWithLock(userId, () -> {
            BoundHashOperations<String, Object, Object> cartOps = getCartOps(userId);
            String cartItemJson = (String) cartOps.get(variantId.toString());

            if (!StringUtils.hasText(cartItemJson)) {
                throw new BusinessException(ResultCodeEnum.CART_ITEM_NOT_FOUND, "商品不在購物車中");
            }

            try {
                CartItemDTO cartItem = objectMapper.readValue(cartItemJson, CartItemDTO.class);
                cartItem.setQuantity(quantity);
                cartOps.put(variantId.toString(), objectMapper.writeValueAsString(cartItem));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "更新失敗");
            }

            return null;
        });

        // 鎖外快速回傳 — 前端會 invalidateQueries 取得完整計價
        return getCartQuick(userId);
    }

    /**
     * 批量移除購物車商品
     * 
     * @param variantIds 商品規格 ID 陣列
     * @return 更新後的購物車內容 (VO)
     */
    @Override
    public CartVO removeItem(Long userId, Long... variantIds) {
        // 鎖內僅做 Redis 刪除
        executeWithLock(userId, () -> {
            BoundHashOperations<String, Object, Object> cartOps = getCartOps(userId);
            Object[] keys = Arrays.stream(variantIds).map(String::valueOf).toArray();
            if (keys.length > 0)
                cartOps.delete(keys);
            return null;
        });

        // 鎖外快速回傳 — 前端會 invalidateQueries 取得完整計價
        return getCartQuick(userId);
    }

    /**
     * 清空當前用戶的購物車
     */
    @Override
    public void clearCart(Long userId) {
        executeWithLock(userId, () -> {
            String cartKey = CART_PREFIX + userId;
            redisTemplate.delete(cartKey);
            return null;
        });
    }

    @Override
    public List<CartItemDTO> getCartItemsByIds(Long userId, List<Long> variantIds) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps(userId);
        List<Object> cartItemJsonList = cartOps
                .multiGet(variantIds.stream().map(String::valueOf).collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(cartItemJsonList)) {
            log.warn("【購物車診斷】內部查詢查無商品: userId={}, variantIds={}", userId, variantIds);
            return List.of();
        }

        return cartItemJsonList.stream()
                .filter(json -> json != null)
                .map(json -> {
                    try {
                        return objectMapper.readValue((String) json, CartItemDTO.class);
                    } catch (JsonProcessingException e) {
                        log.warn("【購物車診斷】購物車項目 JSON 反序列化失敗，該項目將被忽略: json={}", json, e);
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    @Override
    public void clearCartItems(Long userId, List<Long> variantIds) {
        removeItem(userId, variantIds.toArray(new Long[0]));
    }

    // --- Helper & Mapping Logic ---

    private CartVO toVO(CartDTO dto) {
        if (dto == null)
            return null;
        log.info("【購物車診斷】DTO 原始項數: {}, 總金額: {}",
                dto.getItems() != null ? dto.getItems().size() : 0,
                dto.getTotalPrice());
        List<CartItemVO> itemVOs = dto.getItems().stream().map(this::toItemVO).collect(Collectors.toList());
        return CartVO.builder()
                .items(itemVOs)
                .totalQuantity(dto.getTotalQuantity())
                .totalAmount(dto.getTotalPrice())
                .build();
    }

    private CartItemVO toItemVO(CartItemDTO item) {
        if (item == null)
            return null;

        StringBuilder specBuilder = new StringBuilder();
        if (item.getOptions() != null) {
            for (int i = 0; i < item.getOptions().size(); i++) {
                VariantOptionDTO opt = item.getOptions().get(i);
                specBuilder.append(opt.getOptionName()).append(": ").append(opt.getOptionValue());
                if (i < item.getOptions().size() - 1)
                    specBuilder.append(" / ");
            }
        }

        return CartItemVO.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .skuCode(item.getSku())
                .specInfo(specBuilder.length() > 0 ? specBuilder.toString() : DEFAULT_SPEC)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                // 促銷欄位對應
                .originalPrice(item.getOriginalPrice())
                .discountAmount(item.getDiscountAmount())
                .promotionName(item.getPromotionName())
                .promotionType(item.getPromotionType())
                .build();
    }

    private BoundHashOperations<String, Object, Object> getCartOps(Long userId) {
        String cartKey = CART_PREFIX + userId;
        return redisTemplate.boundHashOps(cartKey);
    }

    /**
     * 使用 Redisson 分散式鎖執行業務邏輯，防止並發修改導致的數據不一致。
     */
    private <T> T executeWithLock(Long userId, java.util.function.Supplier<T> logic) {
        String lockKey = "lock:cart:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 嘗試獲取鎖，等待 2 秒，租約時間 10 秒 (鎖內操作已精簡至 < 50ms，2s 足夠應對極端竞爭)
            boolean isLocked = lock.tryLock(2000, 10000, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (isLocked) {
                try {
                    return logic.get();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("【購物車服務】獲取鎖失敗，系統繁忙: userId={}", userId);
                throw new BusinessException(ResultCodeEnum.SERVICE_UNAVAILABLE, "系統繁忙，請稍後再試");
            }
        } catch (InterruptedException e) {
            log.error("【購物車服務】獲取鎖被中斷: userId={}", userId, e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "系統異常");
        }
    }
}