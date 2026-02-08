package com.nameless.mall.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * 運費配置類 (支援 Nacos 熱更新)
 * Prefix: mall.shipping
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mall.shipping")
public class ShippingProperties {

    /** 免運門檻 (預設 1500) */
    private BigDecimal freeThreshold = new BigDecimal("1500");

    /** 宅配運費 (預設 100) */
    private BigDecimal deliveryFee = new BigDecimal("100");

    /** 超商運費 (預設 60) */
    private BigDecimal storeFee = new BigDecimal("60");
}
