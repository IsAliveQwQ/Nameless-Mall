package com.nameless.mall.core.domain;

import com.nameless.mall.core.enums.ResultCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 Result 統一回應格式的所有靜態工廠方法與狀態判斷邏輯。
 * 涵蓋 ok/fail 各重載、customMessage 覆蓋與回退、isSuccess/isFailed 互斥性。
 */
class ResultTest {

    // --- ok() 系列 ---

    @Nested
    @DisplayName("ok() 成功回應")
    class OkTests {

        @Test
        @DisplayName("ok() — code=OK, data=null, message=成功")
        void okWithoutData() {
            Result<Void> result = Result.ok();

            assertEquals("OK", result.getCode());
            assertEquals("成功", result.getMessage());
            assertNull(result.getData());
            assertTrue(result.isSuccess());
            assertFalse(result.isFailed());
        }

        @Test
        @DisplayName("ok(data) — data 正確攜帶")
        void okWithData() {
            Result<String> result = Result.ok("hello");

            assertEquals("OK", result.getCode());
            assertEquals("hello", result.getData());
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("ok(data, message) — 自訂訊息取代預設")
        void okWithDataAndMessage() {
            Result<Integer> result = Result.ok(42, "自訂成功訊息");

            assertEquals("OK", result.getCode());
            assertEquals("自訂成功訊息", result.getMessage());
            assertEquals(42, result.getData());
            assertTrue(result.isSuccess());
        }
    }

    // --- fail() 系列 ---

    @Nested
    @DisplayName("fail() 失敗回應")
    class FailTests {

        @Test
        @DisplayName("fail() — 預設回傳 INTERNAL_ERROR")
        void failDefault() {
            Result<Void> result = Result.fail();

            assertEquals("INTERNAL_ERROR", result.getCode());
            assertEquals("系統內部錯誤", result.getMessage());
            assertNull(result.getData());
            assertFalse(result.isSuccess());
            assertTrue(result.isFailed());
        }

        @Test
        @DisplayName("fail(ResultCodeEnum) — code、message 對應枚舉定義")
        void failWithEnum() {
            Result<Void> result = Result.fail(ResultCodeEnum.PRODUCT_NOT_FOUND);

            assertEquals("PRODUCT_NOT_FOUND", result.getCode());
            assertEquals("商品不存在", result.getMessage());
            assertTrue(result.isFailed());
        }

        @Test
        @DisplayName("fail(enum, customMsg) — 自訂訊息覆蓋枚舉預設")
        void failWithEnumAndCustomMessage() {
            Result<Void> result = Result.fail(ResultCodeEnum.STOCK_INSUFFICIENT, "iPhone 15 庫存不足");

            assertEquals("STOCK_INSUFFICIENT", result.getCode());
            assertEquals("iPhone 15 庫存不足", result.getMessage());
            assertTrue(result.isFailed());
        }

        @Test
        @DisplayName("customMessage 為空字串時回退枚舉預設訊息")
        void failWithEnumAndEmptyMessage() {
            Result<Void> result = Result.fail(ResultCodeEnum.UNAUTHORIZED, "");

            assertEquals("UNAUTHORIZED", result.getCode());
            assertEquals("未認證", result.getMessage());
        }

        @Test
        @DisplayName("customMessage 為 null 時回退枚舉預設訊息")
        void failWithEnumAndNullMessage() {
            Result<Void> result = Result.fail(ResultCodeEnum.UNAUTHORIZED, null);

            assertEquals("UNAUTHORIZED", result.getCode());
            assertEquals("未認證", result.getMessage());
        }

        @Test
        @DisplayName("fail(String) — code 固定 INTERNAL_ERROR")
        void failWithStringMessage() {
            Result<Void> result = Result.fail("自訂錯誤");

            assertEquals("INTERNAL_ERROR", result.getCode());
            assertEquals("自訂錯誤", result.getMessage());
            assertTrue(result.isFailed());
        }

        @Test
        @DisplayName("fail(code, message) — 完全自訂")
        void failWithCodeAndMessage() {
            Result<Void> result = Result.fail("CUSTOM_CODE", "自訂訊息");

            assertEquals("CUSTOM_CODE", result.getCode());
            assertEquals("自訂訊息", result.getMessage());
            assertTrue(result.isFailed());
        }
    }

    // --- 狀態判斷 ---

    @Nested
    @DisplayName("isSuccess / isFailed 判斷")
    class SuccessCheckTests {

        @Test
        @DisplayName("只有 code=OK 時 isSuccess 為 true")
        void onlyOkIsSuccess() {
            assertTrue(Result.ok().isSuccess());
            assertFalse(Result.fail().isSuccess());
            assertFalse(Result.fail(ResultCodeEnum.NOT_FOUND).isSuccess());
        }

        @Test
        @DisplayName("isFailed 與 isSuccess 互斥")
        void failedIsInverseOfSuccess() {
            Result<Void> ok = Result.ok();
            Result<Void> fail = Result.fail();

            assertEquals(!ok.isSuccess(), ok.isFailed());
            assertEquals(!fail.isSuccess(), fail.isFailed());
        }
    }

    // --- Jackson 反序列化 ---

    @Test
    @DisplayName("@JsonCreator 建構子可直接建立 Result 實例")
    void jsonCreatorConstructor() {
        Result<String> result = new Result<>("CUSTOM", "測試", "data");

        assertEquals("CUSTOM", result.getCode());
        assertEquals("測試", result.getMessage());
        assertEquals("data", result.getData());
    }
}
