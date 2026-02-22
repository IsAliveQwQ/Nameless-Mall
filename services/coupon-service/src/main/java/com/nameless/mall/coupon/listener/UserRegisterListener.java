package com.nameless.mall.coupon.listener;

import com.nameless.mall.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 監聽用戶註冊事件，處理相關業務（如發放新人優惠券）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisterListener {

    private final CouponService couponService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "coupon.new-user.queue", durable = "true"), exchange = @Exchange(value = "mall.user.exchange", type = "topic"), key = "user.registered"))
    public void handleUserRegister(Long userId) {
        log.info("收到新用戶註冊事件: userId={}", userId);
        try {
            couponService.distributeNewUserCoupon(userId);
        } catch (Exception e) {
            log.error("處理新用戶註冊事件失敗，準備重試: userId={}", userId, e);
            throw new AmqpException("發放新人優惠券失敗", e);
        }
    }
}
