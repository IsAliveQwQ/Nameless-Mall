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
    public Result<CartVO> getCart() {
        return Result.ok(cartService.getCart());
    }

    @PostMapping("/items")
    public Result<CartVO> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return Result.ok(cartService.addToCart(request.getVariantId(), request.getQuantity()));
    }

    @PutMapping("/items/{variantId}")
    public Result<CartVO> updateItemQuantity(@PathVariable Long variantId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return Result.ok(cartService.updateItemQuantity(variantId, request.getQuantity()));
    }

    @DeleteMapping("/items")
    public Result<CartVO> removeItems(@Valid @RequestBody RemoveItemsRequest request) {
        return Result.ok(cartService.removeItem(request.getVariantIds().toArray(new Long[0])));
    }

    @DeleteMapping
    public Result<Void> clearCart() {
        cartService.clearCart();
        return Result.ok();
    }

    // --- Internal APIs (Return DTO) ---

    @PostMapping("/internal/items")
    public Result<List<CartItemDTO>> getCartItemsByIds(@RequestBody List<Long> cartItemIds) {
        return Result.ok(cartService.getCartItemsByIds(cartItemIds));
    }

    @PostMapping("/internal/clear-items")
    public Result<Void> clearCartItems(@RequestBody List<Long> cartItemIds) {
        cartService.clearCartItems(cartItemIds);
        return Result.ok();
    }
}
