package com.hspyx.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtils {

    // JWT密钥（从配置文件读取，默认值保证至少32字节）
    @Value("${jwt.secret:hspyx-secret-key-for-jwt-token-generation-must-be-long-enough}")
    private String secret;

    // Token过期时间（24小时）
    @Value("${jwt.expiration:86400000}")
    private Long expiration;


    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT token
     * @param username
     * @param role
     * @return JWT字符串
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        // 构建Token
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration)) //过期时间
                .signWith(getSigningKey())
                .compact(); // 生成字符串
    }

    /**
     * 从token中获取用户名
     * @param token
     * @return
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    /**
     * 从Token中获取角色
     * @param token
     * @return
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).getPayload().get("role", String.class);
    }

    /**
     * 验证token有效性
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查token是否过期
     * @param token
     * @return
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getPayload().getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }

    public Long getExpiration() {
        return expiration;
    }
}
