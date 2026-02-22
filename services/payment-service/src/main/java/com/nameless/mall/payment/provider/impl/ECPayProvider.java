package com.nameless.mall.payment.provider.impl;

import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.core.exception.BusinessException;
import com.nameless.mall.payment.api.enums.PaymentMethod;
import com.nameless.mall.payment.api.enums.PaymentProvider;
import com.nameless.mall.payment.api.enums.PaymentStatus;
import com.nameless.mall.payment.config.ECPayConfig;
import com.nameless.mall.payment.provider.AbstractPaymentProvider;
import com.nameless.mall.payment.provider.dto.PaymentCallbackResult;
import com.nameless.mall.payment.provider.dto.PaymentInitContext;
import com.nameless.mall.payment.provider.dto.PaymentInitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 綠界金流 (ECPay) 支付提供商
 * 
 * <p>
 * 整合 ECPay AioCheckOut API (V5)，支援信用卡、ATM、超商代碼等付款方式。
 * </p>
 * 
 * <p>
 * API 文檔: https://developers.ecpay.com.tw/?p=2856
 * </p>
 * 
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ecpay.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class ECPayProvider extends AbstractPaymentProvider {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final int EXPIRE_DAYS = 3; // ATM/超商代碼有效天數
    private static final String SUCCESS_CODE = "1"; // 綠界成功的 RtnCode

    private final ECPayConfig ecPayConfig;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.ECPAY;
    }

    @Override
    public List<PaymentMethod> getSupportedMethods() {
        return List.of(
                PaymentMethod.ECPAY_CREDIT,
                PaymentMethod.ECPAY_ATM,
                PaymentMethod.ECPAY_CVS);
    }

    @Override
    protected PaymentInitResult doInitPayment(PaymentInitContext context) {
        try {
            // 1. 組裝表單參數
            Map<String, String> params = buildCheckOutParams(context);

            // 2. 產生 CheckMacValue
            String checkMacValue = generateCheckMacValue(params);
            params.put("CheckMacValue", checkMacValue);

            // 3. 產生 HTML Form (自動提交)
            String formHtml = buildAutoSubmitForm(params);

            log.info("【ECPay】初始化支付成功 - 訂單: {}, 金額: {}",
                    context.getOrderSn(), context.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValue());

            return PaymentInitResult.builder()
                    .success(true)
                    .status(PaymentStatus.PROCESSING)
                    .redirectType(PaymentInitResult.RedirectType.FORM_POST)
                    .formData(formHtml)
                    .expireAt(LocalDateTime.now().plusDays(EXPIRE_DAYS))
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("【ECPay】初始化支付失敗: {}", e.getMessage(), e);
            throw new BusinessException(ResultCodeEnum.PAYMENT_CREATE_FAILED, "ECPay 支付初始化失敗: " + e.getMessage());
        }
    }

    @Override
    protected PaymentCallbackResult doHandleCallback(Map<String, String> params) {
        log.info("【ECPay】收到回調通知: {}", params);

        // 1. 驗證 CheckMacValue
        String receivedMac = params.get("CheckMacValue");
        Map<String, String> paramsWithoutMac = new HashMap<>(params);
        paramsWithoutMac.remove("CheckMacValue");
        // 移除非綠界原始參數 (PaymentServiceImpl 為 LINE Pay 注入的 amount)
        paramsWithoutMac.remove("amount");

        String calculatedMac = generateCheckMacValue(paramsWithoutMac);
        if (!calculatedMac.equalsIgnoreCase(receivedMac)) {
            log.error("【ECPay】CheckMacValue 驗證失敗 - 預期: {}, 實際: {}", calculatedMac, receivedMac);
            throw new BusinessException(ResultCodeEnum.FORBIDDEN,
                    "ECPay 回調驗證失敗：CheckMacValue 不符");
        }

        // 2. 解析回調結果
        String rtnCode = params.get("RtnCode");
        String rtnMsg = params.get("RtnMsg");
        String tradeNo = params.get("TradeNo");
        String merchantTradeNo = params.get("MerchantTradeNo");

        // 3. 判斷交易是否成功（綠界成功碼為 "1"），失敗則拋出異常
        boolean isSuccess = SUCCESS_CODE.equals(rtnCode);

        if (!isSuccess) {
            log.warn("【ECPay】支付失敗 - 代碼: {}, 訊息: {}", rtnCode, rtnMsg);
            throw new BusinessException(ResultCodeEnum.PAYMENT_FAILED,
                    "ECPay 支付失敗: " + rtnMsg);
        }

        // 4. 建構成功的支付回調結果並回傳
        log.info("【ECPay】支付成功 - 綠界交易編號: {}, 商店訂單編號: {}", tradeNo, merchantTradeNo);

        return PaymentCallbackResult.builder()
                .success(true)
                .status(PaymentStatus.SUCCESS)
                .providerTradeNo(tradeNo)
                .providerResponse(params.toString())
                .paidAt(LocalDateTime.now())
                .build();
    }

    /**
     * 組裝 AioCheckOut 參數
     */
    private Map<String, String> buildCheckOutParams(PaymentInitContext context) {
        Map<String, String> params = new TreeMap<>(); // TreeMap 自動排序

        // 必填參數
        // 綠界 MerchantTradeNo 限制為 20 碼
        String merchantTradeNo = context.getPaymentSn();
        if (merchantTradeNo != null && merchantTradeNo.length() > 20) {
            merchantTradeNo = merchantTradeNo.substring(merchantTradeNo.length() - 20);
            log.warn("【ECPay】PaymentSn 過長，自動截斷為最後 20 碼: {}", merchantTradeNo);
        }
        params.put("MerchantID", ecPayConfig.getMerchantId());
        params.put("MerchantTradeNo", merchantTradeNo); // 商店訂單編號
        params.put("MerchantTradeDate", LocalDateTime.now().format(DATE_FORMATTER));
        params.put("PaymentType", "aio");
        params.put("TotalAmount",
                String.valueOf(context.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValue()));
        params.put("TradeDesc", urlEncode("Nameless Mall 線上購物"));
        params.put("ItemName", truncateItemName(context.getItemName()));
        params.put("ReturnURL", ecPayConfig.getCallbackUrl());
        params.put("OrderResultURL", ecPayConfig.getFrontendReturnUrl() + "?paymentSn=" + context.getPaymentSn());
        params.put("EncryptType", "1"); // SHA256

        // 付款方式：交由綠界頁面讓用戶選擇（業界主流做法）
        // 支援：信用卡、ATM 虛擬帳號、超商代碼
        params.put("ChoosePayment", "ALL");

        return params;
    }

    /**
     * 產生 CheckMacValue (SHA256)
     * 
     * <p>
     * 綠界規範流程：
     * 1. 依參數名稱排序 (A-Z)，忽略空值
     * 2. 組成 key=value 字串
     * 3. 前後加上 HashKey/HashIV
     * 4. 對整個字串 URL Encode
     * 5. 轉小寫
     * 6. 執行 .NET 特殊字元還原
     * 7. SHA256 並轉大寫
     * </p>
     */
    private String generateCheckMacValue(Map<String, String> params) {
        // 1. 過濾空值、排序、組成字串
        String raw = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
                .collect(Collectors.joining("&"));

        // 2. 加上 HashKey 和 HashIV
        raw = "HashKey=" + ecPayConfig.getHashKey() + "&" + raw + "&HashIV=" + ecPayConfig.getHashIv();

        // 3. 對整個字串 URL Encode 並轉小寫
        raw = urlEncode(raw).toLowerCase();

        // 4. 執行 .NET 特殊字元轉換 (綠界規範)
        raw = raw.replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%20", "+");

        // 5. SHA256 並轉大寫
        return sha256(raw).toUpperCase();
    }

    /**
     * 產生自動提交的 HTML 表單
     */
    private String buildAutoSubmitForm(Map<String, String> params) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head>");
        html.append("<body onload='document.forms[0].submit()'>");
        html.append("<form method='POST' action='").append(ecPayConfig.getAioCheckOutUrl()).append("'>");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type='hidden' name='")
                    .append(escapeHtml(entry.getKey()))
                    .append("' value='")
                    .append(escapeHtml(entry.getValue()))
                    .append("'/>");
        }

        html.append("<p>正在跳轉至綠界付款頁面...</p>");
        html.append("<noscript><button type='submit'>點此繼續</button></noscript>");
        html.append("</form></body></html>");

        return html.toString();
    }

    private String truncateItemName(String itemName) {
        if (itemName == null) {
            return "商品購買";
        }
        // 綠界限制 200 字元
        return itemName.length() > 200 ? itemName.substring(0, 200) : itemName;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR, "SHA-256 演算法不支援: " + e.getMessage());
        }
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
