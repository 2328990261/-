package com.wangrui.springboot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储（开发/演示用，生产环境可替换为 Redis 等）
 * 手机号 -> 验证码，带过期时间
 */
public class SmsCodeStore {

    private static final long EXPIRE_MS = 5 * 60 * 1000; // 5 分钟

    private static class Entry {
        String code;
        long expireAt;

        Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    private static final Map<String, Entry> STORE = new ConcurrentHashMap<>();

    public static void put(String phone, String code) {
        STORE.put(phone, new Entry(code, System.currentTimeMillis() + EXPIRE_MS));
    }

    public static boolean verify(String phone, String code) {
        Entry e = STORE.get(phone);
        if (e == null) return false;
        if (System.currentTimeMillis() > e.expireAt) {
            STORE.remove(phone);
            return false;
        }
        boolean ok = code != null && code.equals(e.code);
        if (ok) STORE.remove(phone);
        return ok;
    }
}
