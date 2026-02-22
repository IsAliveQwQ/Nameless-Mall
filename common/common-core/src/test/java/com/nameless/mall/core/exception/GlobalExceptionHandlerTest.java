package com.nameless.mall.core.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 測試 GlobalExceptionHandler 全部 7 個 @ExceptionHandler 方法。
 * 驗證回傳的 HTTP 狀態碼、Result.code、Result.message 是否符合預期。
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(new ObjectMapper());
    }

    // --- BusinessException ---

    @Nested
    @DisplayName("BusinessException 處理")
    class BusinessExceptionTests {

        @Test
        @DisplayName("ResultCodeEnum 建構 — code、message、httpStatus 正確")
        void handleWithResultCodeEnum() {
            BusinessException ex = new BusinessException(ResultCodeEnum.PRODUCT_NOT_FOUND);

            ResponseEntity<Result<?>> response = handler.handleBusinessException(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("PRODUCT_NOT_FOUND", response.getBody().getCode());
            assertEquals("商品不存在", response.getBody().getMessage());
        }

        @Test
        @DisplayName("ResultCodeEnum + 自訂訊息 — message 被覆蓋")
        void handleWithCustomMessage() {
            BusinessException ex = new BusinessException(
                    ResultCodeEnum.STOCK_INSUFFICIENT, "iPhone 15 庫存不足");

            ResponseEntity<Result<?>> response = handler.handleBusinessException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("iPhone 15 庫存不足", response.getBody().getMessage());
        }
    }

    // --- @Valid 參數校驗 ---

    @Nested
    @DisplayName("MethodArgumentNotValid 處理")
    class ValidationTests {

        @Test
        @DisplayName("有 FieldError — 取得欄位層級錯誤訊息")
        void handleWithFieldError() {
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("dto", "name", "名稱不得為空");
            when(bindingResult.getFieldError()).thenReturn(fieldError);

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<Result<?>> response = handler.handleValidationException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("INVALID_ARGUMENT", response.getBody().getCode());
            assertEquals("名稱不得為空", response.getBody().getMessage());
        }

        @Test
        @DisplayName("無 FieldError — 回退為「參數驗證失敗」")
        void handleWithoutFieldError() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldError()).thenReturn(null);

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<Result<?>> response = handler.handleValidationException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("參數驗證失敗", response.getBody().getMessage());
        }
    }

    // --- JSON 解析錯誤 ---

    @Nested
    @DisplayName("HttpMessageNotReadable 處理")
    class JsonParseTests {

        @Test
        @DisplayName("一般 JSON 解析錯誤 — 回傳 400 + JSON 相關訊息")
        void handleGenericJsonError() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error",
                    (Throwable) null, null);

            ResponseEntity<Result<?>> response = handler.handleHttpMessageNotReadableException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("JSON"));
        }

        @Test
        @DisplayName("LocalDateTime 解析失敗 — 提示 yyyy-MM-dd 格式")
        void handleLocalDateTimeParseError() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Cannot deserialize value of type `java.time.LocalDateTime`",
                    (Throwable) null, null);

            ResponseEntity<Result<?>> response = handler.handleHttpMessageNotReadableException(ex);

            assertTrue(response.getBody().getMessage().contains("yyyy-MM-dd"));
        }
    }

    // --- @Validated 路徑/查詢參數校驗 ---

    @Test
    @DisplayName("ConstraintViolation — 組合多個違規訊息為單一字串")
    @SuppressWarnings("unchecked")
    void handleConstraintViolation() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("id");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("必須大於 0");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<Result<?>> response = handler.handleConstraintViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("id"));
        assertTrue(response.getBody().getMessage().contains("必須大於 0"));
    }

    // --- HTTP 方法不支援 ---

    @Test
    @DisplayName("HttpRequestMethodNotSupported — 回傳 405 並包含方法名")
    void handleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<Result<?>> response = handler.handleMethodNotSupportedException(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("DELETE"));
    }

    // --- Feign 跨服務呼叫失敗 ---

    @Nested
    @DisplayName("FeignException 處理")
    class FeignTests {

        private FeignException createFeignException(int status, String body) {
            Request request = Request.create(
                    Request.HttpMethod.GET, "/test", Map.of(), null,
                    new RequestTemplate());
            return FeignException.errorStatus("test",
                    feign.Response.builder()
                            .status(status)
                            .reason("Error")
                            .headers(Collections.emptyMap())
                            .request(request)
                            .body(body, StandardCharsets.UTF_8)
                            .build());
        }

        @Test
        @DisplayName("404 — 解析下游 JSON body 的 message 欄位")
        void handle404() {
            FeignException ex = createFeignException(404,
                    "{\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"商品不存在\"}");

            ResponseEntity<Result<?>> response = handler.handleFeignException(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("NOT_FOUND", response.getBody().getCode());
            assertEquals("商品不存在", response.getBody().getMessage());
        }

        @Test
        @DisplayName("401 — 固定回傳 UNAUTHORIZED")
        void handle401() {
            FeignException ex = createFeignException(401, "");

            ResponseEntity<Result<?>> response = handler.handleFeignException(ex);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("UNAUTHORIZED", response.getBody().getCode());
        }

        @Test
        @DisplayName("400 — 泛指客戶端錯誤，映射為 INVALID_ARGUMENT")
        void handle400() {
            FeignException ex = createFeignException(400,
                    "{\"message\":\"參數錯誤\"}");

            ResponseEntity<Result<?>> response = handler.handleFeignException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("INVALID_ARGUMENT", response.getBody().getCode());
        }

        @Test
        @DisplayName("503 — 映射為 SERVICE_UNAVAILABLE")
        void handle503() {
            FeignException ex = createFeignException(503, "");

            ResponseEntity<Result<?>> response = handler.handleFeignException(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals("SERVICE_UNAVAILABLE", response.getBody().getCode());
        }

        @Test
        @DisplayName("500 — 預設映射為 INTERNAL_ERROR")
        void handle500() {
            FeignException ex = createFeignException(500, "");

            ResponseEntity<Result<?>> response = handler.handleFeignException(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        }

        @Test
        @DisplayName("下游回應非 JSON — 不拋例外，使用預設訊息")
        void handleNonJsonBody() {
            FeignException ex = createFeignException(404, "plain text error");

            ResponseEntity<Result<?>> response = handler.handleFeignException(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody().getMessage());
        }
    }

    // --- 兜底處理 ---

    @Test
    @DisplayName("未捕獲的系統例外 — 500 + 不洩漏內部 stack trace")
    void handleGlobalException() {
        Exception ex = new RuntimeException("NullPointerException 在第 42 行");

        ResponseEntity<Result<?>> response = handler.handleGlobalException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertFalse(response.getBody().getMessage().contains("NullPointerException"));
        assertFalse(response.getBody().getMessage().contains("42"));
    }
}
