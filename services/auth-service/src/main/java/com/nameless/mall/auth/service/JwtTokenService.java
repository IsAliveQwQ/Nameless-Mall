package com.nameless.mall.auth.service;

import com.nameless.mall.auth.entity.SecurityUser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT 核心簽發服務。
 * <p>
 * 使用 RSA RS256 非對稱加密簽發 JWT。
 * 私鑰於系統啟動時從外部 PEM 檔載入並轉換為 PKCS8 格式，避免金鑰 hardcode。
 * 簽發的 Token 交由 Gateway 的公鑰進行統一驗證。
 * </p>
 */
@Service
public class JwtTokenService {

    private final long jwtExpiration;
    private final PrivateKey privateKey;
    private final ResourceLoader resourceLoader;
    private final String keyId;

    public JwtTokenService(@Value("${jwt.private-key-path}") String privateKeyPath,
            @Value("${jwt.expiration}") long jwtExpiration,
            @Value("${jwt.key-id:nameless-mall-auth-key-1}") String keyId,
            ResourceLoader resourceLoader)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.jwtExpiration = jwtExpiration;
        this.resourceLoader = resourceLoader;
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.keyId = keyId;
    }

    private PrivateKey loadPrivateKey(String path)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 1. 載入 PEM 檔案內容
        Resource resource = resourceLoader.getResource(path);
        String keyContent;
        try (var is = resource.getInputStream()) {
            keyContent = new String(is.readAllBytes());
        }

        // 2. 去除 PEM 標頭與空白字元
        keyContent = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // 3. Base64 解碼為位元組陣列
        byte[] decodedKey = Base64.getDecoder().decode(keyContent);
        // 4. 透過 PKCS8 規格建立 RSA 私鑰
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 根據 Authentication 狀態簽發 JWT (Access Token)。
     * <p>
     * 核心邏輯:
     * 1. 從 Principal 中提取 UserID 作為 JWT 的 subject (sub claim)。
     * 2. 附加角色權限 (scope claim) 供下游服務進行粗粒度權限管控。
     * 3. 設定合理過期時間與 kid (Key ID) 以支援未來金鑰輪替 (Key Rotation)。
     * </p>
     *
     * @param authentication 通過 Spring Security 驗證後的物件
     * @return 受到 RSA 簽名保護的 JWT 字串
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        // 獲取權限列表，並用空格連接
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.joining(" "));

        // 從 Authentication 取出 Principal，使用 SecurityUser 的數字 ID 作為 JWT subject
        Object principal = authentication.getPrincipal();
        String subject;

        if (principal instanceof SecurityUser securityUser) {
            subject = String.valueOf(securityUser.getId());
        } else {
            // fallback：Principal 不是 SecurityUser 時，退回使用 username
            subject = authentication.getName();
        }

        return Jwts.builder()
                .header().add("kid", this.keyId).and()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtExpiration)))
                .claim("scope", scope)
                .claim("username", authentication.getName())
                .signWith(privateKey)
                .compact();
    }
}