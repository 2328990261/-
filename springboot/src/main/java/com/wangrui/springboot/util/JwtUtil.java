package com.wangrui.springboot.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类，用于生成和验证token
 */
public class JwtUtil {

    // 过期时间，默认7天
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    // 签名密钥，实际应用中应该从配置文件获取
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * 生成JWT token
     * @param username 用户名
     * @return token字符串
     */
    public static String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 生成带有用户信息的JWT token
     * @param username 用户名
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return token字符串
     */
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
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 验证JWT token是否有效
     * @param token token字符串
     * @return 验证结果
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从token中获取用户名
     * @param token token字符串
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }

    /**
     * 从token中获取用户ID
     * @param token token字符串
     * @return 用户ID
     */
    public static Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Integer.class);
    }

    /**
     * 从token中获取是否为管理员
     * @param token token字符串
     * @return 是否为管理员
     */
    public static Integer getIsAdminFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("isAdmin", Integer.class);
    }

    /**
     * 从token中获取所有Claims
     * @param token token字符串
     * @return Claims对象
     */
    public static Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
