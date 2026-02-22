package com.nameless.mall.payment.provider;

import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentProvider;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付提供商工廠
 * 
 * <p>
 * 根據提供商類型或付款方式選擇對應的策略實作。
 * </p>
 * 
 */
@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final List<PaymentProviderStrategy> strategies;

    /** 提供商類型 → 策略實作的對照表，啟動時初始化 */
    private Map<PaymentProvider, PaymentProviderStrategy> strategyMap;

    @PostConstruct
    void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        PaymentProviderStrategy::getProvider,
                        Function.identity()));
    }

    /**
     * 根據提供商類型取得策略
     * 
     * @param provider 提供商類型
     * @return 對應的策略實作
     * @throws BusinessException 若提供商不支援
     */
    public PaymentProviderStrategy getStrategy(PaymentProvider provider) {
        PaymentProviderStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT,
                    "不支援的金流提供商: " + provider.name());
        }
        return strategy;
    }

    /**
     * 根據付款方式取得策略
     * 
     * @param method 付款方式
     * @return 對應的策略實作
     * @throws BusinessException 若付款方式不支援
     */
    public PaymentProviderStrategy getStrategyByMethod(PaymentMethod method) {
        return strategies.stream()
                .filter(s -> s.getSupportedMethods().contains(method))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.INVALID_ARGUMENT,
                        "不支援的付款方式: " + method.name()));
    }
}
