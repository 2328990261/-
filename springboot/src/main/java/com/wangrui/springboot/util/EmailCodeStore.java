package com.wangrui.springboot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码内存存储（开发/演示用，与 SmsCodeStore 同一模式）。
 * 邮箱 -> 验证码，带过期时间。
 */
public class EmailCodeStore {

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

    public static void put(String email, String code) {
        STORE.put(email.toLowerCase(), new Entry(code, System.currentTimeMillis() + EXPIRE_MS));
    }

    public static boolean verify(String email, String code) {
        Entry e = STORE.get(email.toLowerCase());
        if (e == null) return false;
        if (System.currentTimeMillis() > e.expireAt) {
            STORE.remove(email.toLowerCase());
            return false;
        }
        boolean ok = code != null && code.equals(e.code);
        if (ok) STORE.remove(email.toLowerCase());
        return ok;
    }

    public static void remove(String email) {
        STORE.remove(email.toLowerCase());
    }
}
