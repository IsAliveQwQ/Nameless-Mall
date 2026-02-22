package com.nameless.mall.order.service.impl;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.order.api.dto.FlashSaleSubmitDTO;
import com.nameless.mall.order.api.dto.FlashSaleMessage;
import com.nameless.mall.order.config.FlashSaleQueueConfig;
import com.nameless.mall.order.service.FlashSaleOrderService;
import com.nameless.mall.order.constant.FlashSaleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/** 限時特賣下單服務實作，負責 Redis Lua 扣減庫存與 MQ 消息投遞。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleOrderServiceImpl implements FlashSaleOrderService {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> stockScript;

    @PostConstruct
    public void init() {
        stockScript = new DefaultRedisScript<>();
        stockScript.setLocation(new ClassPathResource("lua/flash_sale_deduct.lua"));
        stockScript.setResultType(Long.class);
    }

    @Override
    public String submitFlashSale(Long userId, FlashSaleSubmitDTO dto) {
        String stockKey = FlashSaleConstants.CACHE_STOCK_PREFIX + dto.getPromotionId() + ":" + dto.getSkuId();
        String orderToken = dto.getOrderToken() != null ? dto.getOrderToken() : UUID.randomUUID().toString();

        // 冪等 Key: 用於 Consumer 防止重複消費，也用於前端查詢狀態
        // 格式: flash_sale:order:{userId}:{skuId}
        String orderStatusKey = FlashSaleConstants.CACHE_ORDER_PREFIX + userId + ":" + dto.getSkuId();

        // 1. 檢查是否重複排隊
        String status = redisTemplate.opsForValue().get(orderStatusKey);
        if (status != null) {
            // 已有紀錄 (可能是 PENDING 或 SUCCESS)
            return orderStatusKey; // 返回相同的 Key 供查詢
        }

        // 2. 執行 LUA 原子性扣減庫存 + 搶占 PENDING 狀態
        // KEYS[1]: Stock Key
        // KEYS[2]: Order Status Key
        Long result = redisTemplate.execute(stockScript,
                java.util.Arrays.asList(stockKey, orderStatusKey),
                String.valueOf(dto.getQuantity()));

        if (result == null || result == -2) {
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "特賣活動未開啟或庫存未預熱");
        }
        if (result == -1) {
            throw new BusinessException(ResultCodeEnum.STOCK_INSUFFICIENT, "特賣庫存已售罄");
        }
        if (result == -3) {
            return orderStatusKey; // 已經排隊中，直接返回
        }

        // 3. 發送異步消息
        FlashSaleMessage message = new FlashSaleMessage();
        message.setUserId(userId);
        message.setPromotionId(dto.getPromotionId());
        message.setSkuId(dto.getSkuId());
        message.setQuantity(dto.getQuantity());
        message.setOrderToken(orderToken);
        message.setReceiverName(dto.getReceiverName());
        message.setReceiverPhone(dto.getReceiverPhone());
        message.setReceiverAddress(dto.getReceiverAddress());
        message.setTimestamp(System.currentTimeMillis());
        message.setPayType(dto.getPayType());
        message.setShippingMethod(dto.getShippingMethod());

        rabbitTemplate.convertAndSend(
                FlashSaleQueueConfig.EXCHANGE_FLASH_SALE,
                FlashSaleQueueConfig.KEY_FLASH_SALE,
                message);

        return orderStatusKey;
    }

    @Override
    public Result<Object> checkOrderResult(String orderToken) {
        // 1. 從 Redis 取得訂單處理狀態（orderToken 即 Redis Key）
        String status = redisTemplate.opsForValue().get(orderToken);

        // 2. 依狀態值分支回傳結果
        if (status == null) {
            return Result.fail("查無訂單紀錄 (可能已過期或被取消)");
        }
        if (FlashSaleConstants.STATUS_PENDING.equals(status)) {
            // PENDING → 仍在排隊處理中
            return Result.ok(null, "排隊中...");
        }
        if (FlashSaleConstants.STATUS_FAILED.equals(status)) {
            // FAILED → 搶購失敗
            return Result.fail("搶購失敗");
        }

        // SUCCESS → 回傳 OrderSn
        return Result.ok(status);
    }
}
