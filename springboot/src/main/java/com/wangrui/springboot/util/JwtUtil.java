package com.wangrui.springboot.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具：签发与校验。签名密钥须由 {@link com.wangrui.springboot.config.JwtConfiguration} 在启动时注入。
 */
public final class JwtUtil {
    //1分钟有效时间
    private static final long EXPIRATION_TIME =7 * 60 * 60 * 1000L;

    private static volatile Key signingKey;

    private JwtUtil() {
    }

    /**
     * 使用配置中的字符串派生 HS256 密钥（SHA-256 摘要，任意长度口令均可）。
     */
    public static void initSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret 不能为空");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 JWT 密钥失败", e);
        }
    }

    private static Key key() {
        if (signingKey == null) {
            throw new IllegalStateException("JWT 尚未初始化，请检查 jwt.secret 与 JwtConfiguration");
        }
        return signingKey;
    }

    public static String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public static String generateToken(String username, Integer userId, Integer isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        claims.put("isAdmin", isAdmin);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getUsernameFromToken(String token) {
        return claims(token).get("username", String.class);
    }

    public static Integer getUserIdFromToken(String token) {
        return toInteger(claims(token).get("userId"));
    }

    public static Integer getIsAdminFromToken(String token) {
        return toInteger(claims(token).get("isAdmin"));
    }

    public static Claims getAllClaimsFromToken(String token) {
        return claims(token);
    }

    private static Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static Integer toInteger(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }
}
