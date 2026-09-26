package com.wangrui.springboot.util;

import java.time.Instant;

public class CacheEntry<T> {
    private T value;
    private Instant cachedAt;

    public CacheEntry() {
    }

    public CacheEntry(T value, Instant cachedAt) {
        this.value = value;
        this.cachedAt = cachedAt;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(Instant cachedAt) {
        this.cachedAt = cachedAt;
    }
}
