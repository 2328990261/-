package com.wangrui.springboot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.config.CacheProperties;
import com.wangrui.springboot.util.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

@Service
public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String KEY_PREFIX = "novel:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    public boolean enabled() {
        return cacheProperties.isEnabled();
    }

    public CacheProperties properties() {
        return cacheProperties;
    }

    public <T> T get(String key, Class<T> type) {
        if (!enabled()) {
            return null;
        }
        String fullKey = namespaced(key);
        try {
            String json = redisTemplate.opsForValue().get(fullKey);
            if (json == null) {
                log.debug("Redis cache miss key={}", fullKey);
                return null;
            }
            T value = objectMapper.readValue(json, type);
            log.debug("Redis cache hit key={}", fullKey);
            return value;
        } catch (Exception e) {
            log.warn("Redis cache read failed key={}", fullKey, e);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> type) {
        if (!enabled()) {
            return null;
        }
        String fullKey = namespaced(key);
        try {
            String json = redisTemplate.opsForValue().get(fullKey);
            if (json == null) {
                log.debug("Redis cache miss key={}", fullKey);
                return null;
            }
            T value = objectMapper.readValue(json, type);
            log.debug("Redis cache hit key={}", fullKey);
            return value;
        } catch (Exception e) {
            log.warn("Redis cache read failed key={}", fullKey, e);
            return null;
        }
    }

    public <T> CacheEntry<T> getEntry(String key, TypeReference<CacheEntry<T>> type) {
        return get(key, type);
    }

    public <T> boolean set(String key, T value, Duration ttl) {
        if (!enabled() || value == null || ttl == null || !ttl.isPositive()) {
            return false;
        }
        String fullKey = namespaced(key);
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(fullKey, json, ttl);
            return true;
        } catch (Exception e) {
            log.warn("Redis cache write failed key={}", fullKey, e);
            return false;
        }
    }

    public <T> boolean setEntry(String key, T value, Duration ttl) {
        return set(key, new CacheEntry<>(value, Instant.now()), ttl);
    }

    public void delete(String... keys) {
        if (!enabled() || keys == null || keys.length == 0) {
            return;
        }
        try {
            String[] fullKeys = Arrays.stream(keys)
                    .filter(key -> key != null && !key.isBlank())
                    .map(this::namespaced)
                    .toArray(String[]::new);
            if (fullKeys.length == 0) {
                return;
            }
            Long deleted = redisTemplate.delete(Arrays.asList(fullKeys));
            log.info("Redis cache invalidation keys={} deleted={}", Arrays.toString(fullKeys), deleted);
        } catch (Exception e) {
            log.warn("Redis cache invalidation failed keys={}", Arrays.toString(keys), e);
        }
    }

    public long increment(String key, Duration ttl) {
        if (!enabled()) {
            return 0L;
        }
        String fullKey = namespaced(key);
        try {
            Long value = redisTemplate.opsForValue().increment(fullKey);
            if (value != null) {
                redisTemplate.expire(fullKey, ttl);
            }
            return value != null ? value : 0L;
        } catch (Exception e) {
            log.warn("Redis increment failed key={}", fullKey, e);
            return 0L;
        }
    }

    public long getUserRecommendationVersion(Integer userId) {
        if (!enabled() || userId == null) {
            return 0L;
        }
        String key = "novel:rec:version:" + userId;
        try {
            String value = redisTemplate.opsForValue().get(namespaced(key));
            if (value != null) {
                return Long.parseLong(value);
            }
            redisTemplate.opsForValue().set(namespaced(key), "1", cacheProperties.getVersionTtl());
            return 1L;
        } catch (Exception e) {
            log.warn("Redis recommendation-version read failed userId={}", userId, e);
            return 0L;
        }
    }

    public long incrementUserRecommendationVersion(Integer userId) {
        if (!enabled() || userId == null) {
            return 0L;
        }
        return increment("novel:rec:version:" + userId, cacheProperties.getVersionTtl());
    }

    private String namespaced(String key) {
        return key != null && key.startsWith(KEY_PREFIX) ? key : KEY_PREFIX + key;
    }
}
