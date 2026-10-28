package com.wangrui.springboot.cache;

import java.time.Instant;

/**
 * 缓存条目实体
 * 用于封装缓存中的一个数据项及其缓存时间
 */
public class CacheEntry<T> {
    // 缓存的实际数据值
    private T value;

    // 数据被缓存的时间
    private Instant cachedAt;

    // 无参构造方法
    public CacheEntry() {
    }

    // 带参构造方法：初始化缓存值与缓存时间
    public CacheEntry(T value, Instant cachedAt) {
        this.value = value;
        this.cachedAt = cachedAt;
    }

    /** 获取缓存值 */
    public T getValue() {
        return value;
    }

    /** 设置缓存值 */
    public void setValue(T value) {
        this.value = value;
    }

    /** 获取缓存时间 */
    public Instant getCachedAt() {
        return cachedAt;
    }

    /** 设置缓存时间 */
    public void setCachedAt(Instant cachedAt) {
        this.cachedAt = cachedAt;
    }
}