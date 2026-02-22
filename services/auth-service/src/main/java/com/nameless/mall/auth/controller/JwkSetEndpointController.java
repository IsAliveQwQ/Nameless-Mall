package com.nameless.mall.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * JWK Set Endpoint Controller
 * 
 * 提供 OAuth2/OIDC 標準的 JWK Set 端點，讓其他微服務可以動態獲取公鑰驗證 JWT。
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 - JSON
 *      Web Key (JWK)</a>
 */
@RestController
public class JwkSetEndpointController {

    private final RSAPublicKey publicKey;
    private final JWKSet jwkSet;
    private final String keyId;

    public JwkSetEndpointController(
            @Value("${jwt.public-key-path:classpath:public_key.pem}") String publicKeyPath,
            @Value("${jwt.key-id:nameless-mall-auth-key-1}") String keyId,
            ResourceLoader resourceLoader) throws Exception {

        this.publicKey = loadPublicKey(publicKeyPath, resourceLoader);
        this.keyId = keyId;
        this.jwkSet = buildJwkSet(this.publicKey);
    }

    /**
     * JWK Set 端點 - 標準 OAuth2 路徑
     * 
     * @return JWK Set JSON (包含 RSA 公鑰)
     */
    @GetMapping(value = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getJwkSet() {
        return jwkSet.toJSONObject();
    }

    /**
     * JWK Set 端點 - OIDC 標準路徑 (別名)
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getWellKnownJwkSet() {
        return getJwkSet();
    }

    /**
     * 從 PEM 檔案載入 RSA 公鑰
     */
    private RSAPublicKey loadPublicKey(String path, ResourceLoader resourceLoader)
            throws Exception {
        Resource resource = resourceLoader.getResource(path);
        String keyContent;
        try (java.io.InputStream is = resource.getInputStream()) {
            keyContent = new String(is.readAllBytes());
        }

        // 移除 PEM 標頭、標尾和空白
        keyContent = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decodedKey = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * 將 RSA 公鑰轉換為 JWK Set 格式
     */
    private JWKSet buildJwkSet(RSAPublicKey publicKey) {
        // 建立 RSA JWK，包含必要的 metadata
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .keyID(this.keyId) // Key ID，用於識別
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .build();

        return new JWKSet(rsaKey);
    }
}
