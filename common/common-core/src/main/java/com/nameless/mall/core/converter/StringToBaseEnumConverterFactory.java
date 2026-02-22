package com.nameless.mall.core.converter;

import com.nameless.mall.core.enums.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * String 轉 BaseEnum 的 Converter Factory。
 * 
 * 讓 Spring MVC 能自動將 URL Query Parameter (String) 轉換成對應的 Enum。
 * 支援兩種 mapping 方式:
 * - 用 code (整數): ?status=1 -> OrderStatus.PAID
 * - 用 name (字串): ?status=PAID -> OrderStatus.PAID
 * 
 * 用法: 不需手動設定，Spring 會自動掃描這個 @Component。
 * 只要 Enum 實作 BaseEnum 介面，就能自動轉換。
 * 
 * @see BaseEnum
 */
@Component
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @SuppressWarnings("rawtypes")
    private static final Map<Class, Converter> CONVERTER_CACHE = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return CONVERTER_CACHE.computeIfAbsent(targetType, StringToEnumConverter::new);
    }

    private static class StringToEnumConverter<T extends BaseEnum> implements Converter<String, T> {

        private final Map<String, T> enumMap = new HashMap<>();

        public StringToEnumConverter(Class<T> enumType) {
            T[] enums = enumType.getEnumConstants();
            for (T e : enums) {
                enumMap.put(String.valueOf(e.getCode()), e);
                // 同時支援用 Enum name 來查找，例如 ?status=PAID
                enumMap.put(((Enum<?>) e).name().toUpperCase(), e);
            }
        }

        @Override
        public T convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            T result = enumMap.get(source.trim().toUpperCase());
            if (result == null) {
                throw new IllegalArgumentException(
                        String.format("無效的列舉值 '%s'，允許的值: %s", source, enumMap.keySet()));
            }
            return result;
        }
    }
}