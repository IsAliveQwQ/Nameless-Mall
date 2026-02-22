package com.nameless.mall.core.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;

/**
 * 全域 Exception Handler，位於 common-core，所有微服務自動套用。
 * 統一將各類例外轉換為 Result 格式回應。
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    /**
     * 處理 BusinessException (業務邏輯錯誤)。
     * 
     * 業務 exception 是「可預期的錯誤」，例如用戶輸入無效、資源不存在等。
     * HTTP 狀態碼會根據 BusinessException 中的 httpStatus 設定。
     *
     * @param ex 業務 exception
     * @return 包含 Result 的 ResponseEntity，HTTP 狀態碼根據 exception 設定
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException ex) {
        log.warn("【業務例外】code={}, message={}", ex.getCode(), ex.getMessage());

        Result<?> resultBody = Result.fail(ex.getResultCode(), ex.getMessage());
        HttpStatus status = HttpStatus.valueOf(ex.getHttpStatus());
        return new ResponseEntity<>(resultBody, status);
    }

    /**
     * 處理 @Valid 校驗失敗 exception。
     * 
     * 當 Controller 方法參數標註了 @Valid，但請求資料不符合 DTO 中的
     * 驗證註解 (如 @NotBlank, @Size 等) 時，Spring 會 throw 此 exception。
     * 
     * @param ex 參數驗證失敗 exception
     * @return 包含驗證錯誤訊息的 Result，HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = "參數驗證失敗";
        if (ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        log.warn("【參數校驗】MethodArgumentNotValid: {}", message);

        Result<?> resultBody = Result.fail(ResultCodeEnum.INVALID_ARGUMENT, message);
        return new ResponseEntity<>(resultBody, HttpStatus.BAD_REQUEST);
    }

    /**
     * 處理 JSON 解析 exception (如格式錯誤、反序列化失敗)。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.error("JSON 解析失敗: {}", ex.getMessage());
        String msg = "請求資料格式錯誤 (JSON Parsing Error)";
        // 如果是日期解析失敗，給出提示
        if (ex.getMessage() != null && ex.getMessage().contains("LocalDateTime")) {
            msg = "時間格式解析失敗，請採用 yyyy-MM-dd'T'HH:mm:ss 格式";
        }
        return new ResponseEntity<>(Result.fail(ResultCodeEnum.INVALID_ARGUMENT, msg), HttpStatus.BAD_REQUEST);
    }

    /**
     * 處理 @Validated 路徑/查詢參數校驗失敗。
     * <p>
     * Controller 類別標註 @Validated 後，@PathVariable / @RequestParam 上的
     * 約束 (@Min, @NotNull 等) 不滿足時會拋出此 exception（不同於 @RequestBody 的
     * MethodArgumentNotValidException）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<?>> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("參數驗證失敗");
        log.warn("【參數校驗】ConstraintViolation: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCodeEnum.INVALID_ARGUMENT, message), HttpStatus.BAD_REQUEST);
    }

    /**
     * 處理 HTTP 方法不支援（如 POST 打到 GET 端點）。
     * <p>
     * 設計決策：ResultCodeEnum 無獨立的 METHOD_NOT_ALLOWED code，
     * 此類錯誤本質屬於「用戶端呼叫方式有誤」，故 code 沿用 INVALID_ARGUMENT，
     * 但 HTTP 狀態碼明確回傳 405 METHOD_NOT_ALLOWED，而非 400。
     * 如此前端可依 HTTP 狀態碼區分錯誤類型，同時保持 code 語意可讀。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<?>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        String message = "不支援 " + ex.getMethod() + " 方法，允許的方法: "
                + (ex.getSupportedHttpMethods() != null ? ex.getSupportedHttpMethods() : "N/A");
        log.warn("【HTTP 405】{}", message);
        return new ResponseEntity<>(Result.fail(ResultCodeEnum.INVALID_ARGUMENT, message),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 處理 Feign 呼叫例外 (如 404 Not Found, 400 Bad Request)。
     * 
     * 當呼叫下游微服務失敗時，Feign 會拋出 FeignException。
     * 這裡會根據狀態碼回傳對應的業務結果碼。
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Result<?>> handleFeignException(FeignException ex) {
        log.error("【Feign 呼叫失敗】status={}, message={}", ex.status(), ex.getMessage());

        String msg = "下游服務暫時不可用";
        ResultCodeEnum code = ResultCodeEnum.INTERNAL_ERROR;

        // 1. 提取下游回應 body
        try {
            String content = ex.contentUTF8();
            if (content != null && !content.isEmpty()) {
                // 使用注入的 ObjectMapper 安全解析 JSON
                try {
                    JsonNode root = objectMapper.readTree(content);
                    JsonNode messageNode = root.get("message");
                    if (messageNode != null && messageNode.isTextual()) {
                        msg = messageNode.asText();
                    }
                } catch (Exception jsonEx) {
                    log.debug("下游回應非標準 JSON 格式，保留預設訊息", jsonEx);
                }
            }
        } catch (Exception parseEx) {
            log.warn("解析下游錯誤訊息失敗", parseEx);
        }

        // 3. 依 HTTP 狀態碼映射業務結果碼
        if (ex.status() == 404) {
            code = ResultCodeEnum.NOT_FOUND;
            if (msg.equals("下游服務暫時不可用"))
                msg = "請求的資源不存在";
        } else if (ex.status() == 401) {
            code = ResultCodeEnum.UNAUTHORIZED;
            msg = "授權失敗";
        } else if (ex.status() >= 400 && ex.status() < 500) {
            code = ResultCodeEnum.INVALID_ARGUMENT; // 泛指客戶端錯誤
        } else if (ex.status() == 503) {
            code = ResultCodeEnum.SERVICE_UNAVAILABLE;
        }

        Result<?> resultBody = Result.fail(code, msg);
        // 4. 防禦性處理：status 可能為 -1 或非標準值，回退為 500
        int statusInt = ex.status() > 0 ? ex.status() : 500;
        HttpStatus httpStatus;
        try {
            httpStatus = HttpStatus.valueOf(statusInt);
        } catch (IllegalArgumentException e) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(resultBody, httpStatus);
    }

    /**
     * 處理所有未捕獲的系統 exception (兜底處理)。
     * 
     * 確保任何未預期的錯誤都不會以 stack trace 形式暴露給 client。
     * 回應只包含通用訊息，詳細資訊記錄在 log 中供問題排查。
     *
     * @param ex 未捕獲的 exception
     * @return 通用錯誤 Result，HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleGlobalException(Exception ex) {
        log.error("【未捕獲系統例外】類型: {}, 訊息: {}", ex.getClass().getName(), ex.getMessage(), ex);

        // 只回傳通用訊息，詳細資訊已記錄在 log 中
        String errorMsg = "系統發生錯誤，請稍後重試";
        Result<?> resultBody = Result.fail(ResultCodeEnum.INTERNAL_ERROR, errorMsg);
        return new ResponseEntity<>(resultBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
