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

@Service
public class JwtTokenService {

    private final long jwtExpiration;
    private final PrivateKey privateKey;
    private final ResourceLoader resourceLoader;

    public JwtTokenService(@Value("${jwt.private-key-path}") String privateKeyPath,
            @Value("${jwt.expiration}") long jwtExpiration,
            ResourceLoader resourceLoader)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.jwtExpiration = jwtExpiration;
        this.resourceLoader = resourceLoader;
        this.privateKey = loadPrivateKey(privateKeyPath);
    }

    private PrivateKey loadPrivateKey(String path)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 1. 載入 PEM 檔案內容
        Resource resource = resourceLoader.getResource(path);
        String keyContent = new String(resource.getInputStream().readAllBytes());

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
     * 根據 Authentication 物件產生 JWT，以 SecurityUser 的數字 ID 作為 subject。
     *
     * @param authentication 登入成功後的認證物件，Principal 為 SecurityUser
     * @return JWT 字串
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

        if (principal instanceof SecurityUser) {
            SecurityUser securityUser = (SecurityUser) principal;
            subject = String.valueOf(securityUser.getId());
        } else {
            // fallback：Principal 不是 SecurityUser 時，退回使用 username
            subject = authentication.getName();
        }

        return Jwts.builder()
                .header().add("kid", "nameless-mall-auth-key-1").and()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtExpiration)))
                .claim("scope", scope)
                .claim("username", authentication.getName())
                .signWith(privateKey)
                .compact();
    }
}