package com.nameless.mall.core.converter;

import com.nameless.mall.core.enums.BaseEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 StringToBaseEnumConverterFactory 的轉換邏輯。
 * 涵蓋 code 轉換、name 轉換（不分大小寫）、null/空值處理、無效值例外、trim、Converter 快取。
 */
class StringToBaseEnumConverterFactoryTest {

    private StringToBaseEnumConverterFactory factory;

    /** 測試用枚舉，模擬實際專案中實作 BaseEnum 的 Enum */
    enum TestStatus implements BaseEnum {
        ACTIVE(1),
        INACTIVE(0),
        DELETED(-1);

        private final Integer code;

        TestStatus(Integer code) {
            this.code = code;
        }

        @Override
        public Integer getCode() {
            return code;
        }
    }

    @BeforeEach
    void setUp() {
        factory = new StringToBaseEnumConverterFactory();
    }

    // --- 用整數 code 轉換 ---

    @Nested
    @DisplayName("用 code (整數) 轉換")
    class CodeConversionTests {

        @Test
        @DisplayName("code=1 → ACTIVE")
        void convertByCodeActive() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.ACTIVE, converter.convert("1"));
        }

        @Test
        @DisplayName("code=0 → INACTIVE")
        void convertByCodeInactive() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.INACTIVE, converter.convert("0"));
        }

        @Test
        @DisplayName("code=-1 → DELETED")
        void convertByCodeNegative() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.DELETED, converter.convert("-1"));
        }
    }

    // --- 用 Enum name 轉換 ---

    @Nested
    @DisplayName("用 name (字串) 轉換，不分大小寫")
    class NameConversionTests {

        @Test
        @DisplayName("ACTIVE — 大寫完全匹配")
        void convertByNameUpperCase() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.ACTIVE, converter.convert("ACTIVE"));
        }

        @Test
        @DisplayName("active — 小寫也能匹配")
        void convertByNameLowerCase() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.ACTIVE, converter.convert("active"));
        }

        @Test
        @DisplayName("AcTiVe — 混合大小寫也能匹配")
        void convertByNameMixedCase() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.ACTIVE, converter.convert("AcTiVe"));
        }
    }

    // --- 邊界與例外情境 ---

    @Nested
    @DisplayName("邊界處理與例外")
    class EdgeCaseTests {

        @Test
        @DisplayName("null → 回傳 null")
        void convertNull() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertNull(converter.convert(null));
        }

        @Test
        @DisplayName("空字串 → 回傳 null")
        void convertEmpty() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertNull(converter.convert(""));
        }

        @Test
        @DisplayName("純空格 → 回傳 null")
        void convertBlank() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertNull(converter.convert("   "));
        }

        @Test
        @DisplayName("無效值 → 拋出 IllegalArgumentException，訊息包含原始輸入")
        void convertInvalidValue() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.convert("NONEXISTENT"));

            assertTrue(ex.getMessage().contains("NONEXISTENT"));
        }

        @Test
        @DisplayName("前後有空格 — 自動 trim 後正確轉換")
        void convertWithWhitespace() {
            Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);
            assertEquals(TestStatus.ACTIVE, converter.convert("  ACTIVE  "));
        }
    }

    // --- Converter 快取機制 ---

    @Test
    @DisplayName("同一 Enum 類型回傳同一個 Converter 實例")
    void converterIsCached() {
        Converter<String, TestStatus> c1 = factory.getConverter(TestStatus.class);
        Converter<String, TestStatus> c2 = factory.getConverter(TestStatus.class);
        assertSame(c1, c2);
    }
}
