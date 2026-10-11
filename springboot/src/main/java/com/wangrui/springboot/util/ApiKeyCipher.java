package com.wangrui.springboot.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AI 助手 API Key 的加解密工具（AES-256-GCM）。
 * 数据库中只保存带前缀的密文（enc:v1:base64(iv+ciphertext)），明文不出库。
 * 密钥来自配置 ai.api-key-secret（可用环境变量 AI_API_KEY_SECRET 覆盖）。
 */
@Component
public class ApiKeyCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public ApiKeyCipher(@Value("${ai.api-key-secret:novel-ai-key-secret-change-me}") String secret) {
        this.key = deriveKey(secret);
    }

    /**
     * 加密明文，返回带前缀的 Base64 密文。
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] packed = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, packed, IV_LENGTH, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (Exception e) {
            throw new IllegalStateException("加密 API Key 失败", e);
        }
    }

    /**
     * 解密数据库中的值；带 enc:v1: 前缀时解密，否则按历史明文原样返回；
     * 解密失败返回 null（由调用方回退到环境变量）。
     */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        String value = stored.trim();
        if (!value.startsWith(PREFIX)) {
            return value;
        }
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(cipherBytes, 0, iv, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes, IV_LENGTH, cipherBytes.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 用于后台页面展示的脱敏值。
     */
    public String mask(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        String value = plainText.trim();
        if (value.length() <= 8) {
            return "******";
        }
        return value.substring(0, 4) + "…" + value.substring(value.length() - 4);
    }

    private static SecretKeySpec deriveKey(String secret) {
        String passphrase = (secret == null || secret.isBlank()) ? "novel-ai-key-secret-change-me" : secret;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化 AI API Key 加密失败", e);
        }
    }
}
