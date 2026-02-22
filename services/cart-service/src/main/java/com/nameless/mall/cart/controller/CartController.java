package com.nameless.mall.cart.controller;

import com.nameless.mall.cart.api.dto.CartItemDTO;
import com.nameless.mall.cart.api.vo.CartVO;
import com.nameless.mall.cart.service.CartService;
import com.nameless.mall.core.domain.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 購物車 Controller
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // --- 內部 Request DTO ---
    @Data
    static class AddToCartRequest {
        @NotNull(message = "商品規格 ID 不能為空")
        private Long variantId;
        @NotNull(message = "數量不能為空")
        @Min(value = 1, message = "數量至少為 1")
        private Integer quantity;
    }

    @Data
    static class UpdateQuantityRequest {
        @NotNull(message = "數量不能為空")
        @Min(value = 1, message = "數量至少為 1")
        private Integer quantity;
    }

    @Data
    static class RemoveItemsRequest {
        @NotEmpty(message = "請選擇要刪除的商品")
        private List<Long> variantIds;
    }

    // --- External APIs (Return VO) ---

    @GetMapping
    public Result<CartVO> getCart(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public Result<CartVO> addToCart(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddToCartRequest request) {
        return Result.ok(cartService.addToCart(userId, request.getVariantId(), request.getQuantity()));
    }

    @PutMapping("/items/{variantId}")
    public Result<CartVO> updateItemQuantity(@RequestHeader("X-User-Id") Long userId, @PathVariable Long variantId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return Result.ok(cartService.updateItemQuantity(userId, variantId, request.getQuantity()));
    }

    @DeleteMapping("/items")
    public Result<CartVO> removeItems(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RemoveItemsRequest request) {
        return Result.ok(cartService.removeItem(userId, request.getVariantIds().toArray(new Long[0])));
    }

    @DeleteMapping
    public Result<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return Result.ok();
    }

    // --- Internal APIs (Return DTO) ---

    @PostMapping("/internal/items")
    public Result<List<CartItemDTO>> getCartItemsByIds(@RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> cartItemIds) {
        return Result.ok(cartService.getCartItemsByIds(userId, cartItemIds));
    }

    @PostMapping("/internal/clear-items")
    public Result<Void> clearCartItems(@RequestHeader("X-User-Id") Long userId, @RequestBody List<Long> cartItemIds) {
        cartService.clearCartItems(userId, cartItemIds);
        return Result.ok();
    }
}
