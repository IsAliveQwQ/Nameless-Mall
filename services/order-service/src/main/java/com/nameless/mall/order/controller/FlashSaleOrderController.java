package com.nameless.mall.order.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.order.api.dto.FlashSaleSubmitDTO;
import com.nameless.mall.order.service.FlashSaleOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.*;

/** 限時特賣下單 API，接收前端搶購請求。 */
@Slf4j
@RestController
@RequestMapping("/promotions/flash-sales")
@RequiredArgsConstructor
public class FlashSaleOrderController {

    private final FlashSaleOrderService flashSaleOrderService;

    @PostMapping("/{skuId}/submit")
    @SentinelResource(value = "submitFlashSale", blockHandler = "submitFlashSaleBlock")
    public Result<String> submitFlashSale(
            @PathVariable Long skuId,
            @RequestBody @Valid FlashSaleSubmitDTO submitDTO,
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {

        if (!skuId.equals(submitDTO.getSkuId())) {
            return Result.fail("商品 ID 不一致");
        }

        // 執行特賣下單
        String orderToken = flashSaleOrderService.submitFlashSale(userId, submitDTO);

        // 回傳排隊 Token (前端可用此 Token 輪詢狀態)
        return Result.ok(orderToken);
    }

    /** Sentinel 限流降級：特賣搶購 */
    public Result<String> submitFlashSaleBlock(Long skuId, FlashSaleSubmitDTO submitDTO,
            Long userId, BlockException ex) {
        return Result.fail("系統繁忙，搶購人數過多，請稍後再試");
    }

    @GetMapping("/result")
    public Result<Object> checkResult(@RequestParam String orderToken) {
        return flashSaleOrderService.checkOrderResult(orderToken);
    }
}
