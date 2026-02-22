package com.nameless.mall.payment.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentProvider;
import com.nameless.mall.payment.api.enums.PaymentStatus;
import com.nameless.mall.payment.provider.AbstractPaymentProvider;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;
import com.nameless.mall.payment.provider.dto.PaymentInitContext;
import com.nameless.mall.payment.provider.dto.PaymentInitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LINE Pay 支付提供商
 * 
 * <p>
 * 整合 LINE Pay v3 API (Sandbox 環境)。
 * </p>
 * 
 * <p>
 * API 文檔: https://pay.line.me/documents/online_v3.html
 * </p>
 * 
 */
@Slf4j
@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
@ConditionalOnProperty(name = "payment.linepay.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class LinePayProvider extends AbstractPaymentProvider {

    private static final int EXPIRE_MINUTES = 20;
    private static final String SUCCESS_CODE = "0000"; // LINE Pay 成功的 returnCode

    @Value("${payment.linepay.channel-id}")
    private String channelId;

    @Value("${payment.linepay.channel-secret}")
    private String channelSecret;

    @Value("${payment.linepay.api-url:https://sandbox-api-pay.line.me}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.LINEPAY;
    }

    @Override
    public List<PaymentMethod> getSupportedMethods() {
        return List.of(PaymentMethod.LINE_PAY);
    }

    @Override
    protected PaymentInitResult doInitPayment(PaymentInitContext context) {
        try {
            // 1. 組裝 Request Body
            Map<String, Object> requestBody = buildRequestBody(context);
            String bodyJson = objectMapper.writeValueAsString(requestBody);

            // 2. 產生簽章
            String nonce = UUID.randomUUID().toString();
            String requestUri = "/v3/payments/request";
            String signature = generateSignature(channelSecret, requestUri, bodyJson, nonce);

            // 3. 呼叫 LINE Pay Request API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-LINE-ChannelId", channelId);
            headers.set("X-LINE-Authorization-Nonce", nonce);
            headers.set("X-LINE-Authorization", signature);

            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + requestUri,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            // 4. 解析回應
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !SUCCESS_CODE.equals(responseBody.get("returnCode"))) {
                String returnCode = responseBody != null ? (String) responseBody.get("returnCode") : "UNKNOWN";
                String returnMessage = responseBody != null ? (String) responseBody.get("returnMessage") : "Empty Body";
                throw new BusinessException(ResultCodeEnum.PAYMENT_CREATE_FAILED,
                        "[" + returnCode + "] " + returnMessage);
            }

            Map<String, Object> info = (Map<String, Object>) responseBody.get("info");
            String paymentUrl = ((Map<String, String>) info.get("paymentUrl")).get("web");
            String transactionId = String.valueOf(info.get("transactionId"));

            return PaymentInitResult.builder()
                    .success(true)
                    .status(PaymentStatus.PROCESSING)
                    .redirectType(PaymentInitResult.RedirectType.URL_REDIRECT)
                    .redirectUrl(paymentUrl)
                    .providerTradeNo(transactionId)
                    .expireAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES))
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("【LINE Pay】初始化支付失敗: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.PAYMENT_CREATE_FAILED, "LINE Pay 啟動失敗: " + e.getMessage());
        }
    }

    @Override
    protected PaymentCallbackResult doHandleCallback(Map<String, String> params) {
        // 1. 從回調參數中提取 transactionId 與支付金額
        String transactionId = params.get("transactionId");
        String amountStr = params.get("amount");

        if (transactionId == null || amountStr == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_ARGUMENT, "缺少必要參數: transactionId 或 amount");
        }

        try {
            // 2. 組裝 Confirm API 請求 Body（金額 + 幣別）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", Integer.parseInt(amountStr));
            requestBody.put("currency", "TWD");
            String bodyJson = objectMapper.writeValueAsString(requestBody);

            // 3. 產生 HMAC-SHA256 簽章
            String nonce = UUID.randomUUID().toString();
            String requestUri = "/v3/payments/" + transactionId + "/confirm";
            String signature = generateSignature(channelSecret, requestUri, bodyJson, nonce);

            // 4. 呼叫 LINE Pay Confirm API 確認交易
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-LINE-ChannelId", channelId);
            headers.set("X-LINE-Authorization-Nonce", nonce);
            headers.set("X-LINE-Authorization", signature);

            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + requestUri,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            // 5. 驗證 API 回應狀態並建構支付回調結果
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !SUCCESS_CODE.equals(responseBody.get("returnCode"))) {
                String errorMsg = responseBody != null ? (String) responseBody.get("returnMessage")
                        : "LINE Pay Confirm API 回應失敗";
                throw new BusinessException(ResultCodeEnum.PAYMENT_CHANNEL_ERROR, errorMsg);
            }

            return PaymentCallbackResult.builder()
                    .success(true)
                    .status(PaymentStatus.SUCCESS)
                    .providerTradeNo(transactionId)
                    .providerResponse(objectMapper.writeValueAsString(responseBody))
                    .paidAt(LocalDateTime.now())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("【LINE Pay】回調驗證失敗: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.PAYMENT_CHANNEL_ERROR, "LINE Pay 驗證過程出錯: " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(PaymentInitContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", context.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValue());
        body.put("currency", "TWD");
        body.put("orderId", context.getPaymentSn());

        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("id", "1");
        pkg.put("amount", context.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValue());
        pkg.put("name", "Shop Package");

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("name", context.getItemName() != null ? context.getItemName() : "Mall Product");
        product.put("quantity", 1);
        product.put("price", context.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValue());
        pkg.put("products", List.of(product));

        body.put("packages", List.of(pkg));

        Map<String, String> redirectUrls = new LinkedHashMap<>();
        redirectUrls.put("confirmUrl", context.getReturnUrl() + "?paymentSn=" + context.getPaymentSn());
        redirectUrls.put("cancelUrl", context.getReturnUrl() + "?cancelled=true");
        body.put("redirectUrls", redirectUrls);

        return body;
    }

    private String generateSignature(String secret, String uri, String body, String nonce) {
        try {
            String data = secret + uri + body + nonce;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "LINE Pay 簽章生成失敗: " + e.getMessage());
        }
    }
}
