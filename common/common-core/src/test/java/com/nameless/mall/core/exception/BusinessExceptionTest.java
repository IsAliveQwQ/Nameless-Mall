package com.nameless.mall.core.exception;

import com.nameless.mall.core.enums.ResultCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 BusinessException 兩種建構子的欄位正確性與 httpStatus 對應邏輯。
 */
class BusinessExceptionTest {

    // --- ResultCodeEnum 建構子 ---

    @Nested
    @DisplayName("ResultCodeEnum 建構子")
    class EnumConstructorTests {

        @Test
        @DisplayName("resultCode / code / httpStatus / message 全欄位正確對應")
        void constructWithEnum() {
            BusinessException ex = new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND);

            assertEquals(ResultCodeEnum.PRODUCT_NOT_FOUND, ex.getResultCode());
            assertEquals("PRODUCT_NOT_FOUND", ex.getCode());
            assertEquals(404, ex.getHttpStatus());
            assertEquals("商品不存在", ex.getMessage());
        }

        @Test
        @DisplayName("customMessage 覆蓋枚舉預設訊息")
        void constructWithEnumAndCustomMessage() {
            BusinessException ex = new BusinessException(
                    ResultCodeEnum.STOCK_INSUFFICIENT, "iPhone 15 庫存不足");

            assertEquals(ResultCodeEnum.STOCK_INSUFFICIENT, ex.getResultCode());
            assertEquals("STOCK_INSUFFICIENT", ex.getCode());
            assertEquals(400, ex.getHttpStatus());
            assertEquals("iPhone 15 庫存不足", ex.getMessage());
        }

        @Test
        @DisplayName("httpStatus 對應構建的 ResultCodeEnum 定義")
        void httpStatusMappings() {
            assertEquals(200, new BusinessException(ResultCodeEnum.OK).getHttpStatus());
            assertEquals(401, new BusinessException(ResultCodeEnum.UNAUTHORIZED).getHttpStatus());
            assertEquals(500, new BusinessException(ResultCodeEnum.INTERNAL_ERROR).getHttpStatus());
            assertEquals(503, new BusinessException(ResultCodeEnum.SERVICE_UNAVAILABLE).getHttpStatus());
        }

        @Test
        @DisplayName("super.getMessage() 與 BusinessException.getMessage() 一致")
        void superMessageMatchesCustomMessage() {
            BusinessException ex1 = new BusinessException(ResultCodeEnum.NOT_FOUND);
            assertEquals(ex1.getMessage(), ((Exception) ex1).getMessage());

            BusinessException ex2 = new BusinessException(ResultCodeEnum.NOT_FOUND, "自訂");
            assertEquals("自訂", ((Exception) ex2).getMessage());
        }
    }
}
