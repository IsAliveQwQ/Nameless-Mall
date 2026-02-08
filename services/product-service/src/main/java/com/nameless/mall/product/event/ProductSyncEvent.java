package com.nameless.mall.product.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 商品同步事件 (Spring Internal Event)
 * 符合 SOLID 的 SRP (單一職責)，解耦業務邏輯與訊息發送
 */
@Getter
public class ProductSyncEvent extends ApplicationEvent {

    private final Long productId;
    private final String action; // "UPDATE" or "DELETE"

    public ProductSyncEvent(Object source, Long productId, String action) {
        super(source);
        this.productId = productId;
        this.action = action;
    }
}
