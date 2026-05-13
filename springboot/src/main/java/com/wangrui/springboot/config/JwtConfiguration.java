package com.wangrui.springboot.config;

import com.wangrui.springboot.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 启动时从配置注入 JWT 签名密钥，避免每次重启随机密钥导致已签发 token 全部失效。
 */
@Configuration
public class JwtConfiguration {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void initJwtSigningKey() {
        JwtUtil.initSigningKey(jwtSecret);
    }
}
