package com.wangrui.springboot.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.config.CacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * Redis缓存服务
 * 统一封装基于Redis的缓存读写、失效、自增等操作，支持开关、命名空间和自动过期
 */
@Service
public class RedisCacheService {
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    // 缓存键统一前缀
    private static final String KEY_PREFIX = "novel:";

    // Redis字符串模板（实际上层仍以JSON字符串存储/读取）
    private final StringRedisTemplate redisTemplate;
    // JSON序列化/反序列化工具
    private final ObjectMapper objectMapper;
    // 缓存配置（开关、TTL等）
    private final CacheProperties cacheProperties;

    // 构造方法：注入依赖
    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    /** Redis缓存是否启用 */
    public boolean enabled() {
        return cacheProperties.isEnabled();
    }

    /** 获取缓存配置对象 */
    public CacheProperties properties() {
        return cacheProperties;
    }

    /** 读取缓存（按Class类型反序列化）；未启用或无缓存时返回null */
    public <T> T get(String key, Class<T> type) {
        // 1. 缓存未启用则直接返回null
        if (!enabled()) {
            return null;
        }
        // 2. 拼接带命名空间的完整键
        String fullKey = namespaced(key);
        try {
            // 3. 从Redis读取JSON字符串
            String json = redisTemplate.opsForValue().get(fullKey);
            if (json == null) {
                log.debug("Redis cache miss key={}", fullKey);
                return null;
            }
            // 4. 反序列化为目标类型
            T value = objectMapper.readValue(json, type);
            log.debug("Redis cache hit key={}", fullKey);
            return value;
        } catch (Exception e) {
            // 5. 读取失败记录警告并返回null
            log.warn("Redis cache read failed key={}", fullKey, e);
            return null;
        }
    }

    /** 读取缓存（按TypeReference泛型类型反序列化） */
    public <T> T get(String key, TypeReference<T> type) {
        // 1. 缓存未启用则直接返回null
        if (!enabled()) {
            return null;
        }
        // 2. 拼接带命名空间的完整键
        String fullKey = namespaced(key);
        try {
            // 3. 从Redis读取JSON字符串
            String json = redisTemplate.opsForValue().get(fullKey);
            if (json == null) {
                log.debug("Redis cache miss key={}", fullKey);
                return null;
            }
            // 4. 反序列化为目标类型
            T value = objectMapper.readValue(json, type);
            log.debug("Redis cache hit key={}", fullKey);
            return value;
        } catch (Exception e) {
            // 5. 读取失败记录警告并返回null
            log.warn("Redis cache read failed key={}", fullKey, e);
            return null;
        }
    }

    /** 读取缓存并把结果包装为CacheEntry（带缓存时间） */
    public <T> CacheEntry<T> getEntry(String key, TypeReference<CacheEntry<T>> type) {
        return get(key, type);
    }

    /** 写入缓存：值非空且TTL为正数时才写入，成功返回true */
    public <T> boolean set(String key, T value, Duration ttl) {
        // 1. 校验缓存开关、值和有效期
        if (!enabled() || value == null || ttl == null || !ttl.isPositive()) {
            return false;
        }
        // 2. 拼接完整键
        String fullKey = namespaced(key);
        try {
            // 3. 序列化对象并写入Redis，带上过期时间
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(fullKey, json, ttl);
            return true;
        } catch (Exception e) {
            // 4. 写入失败记录警告
            log.warn("Redis cache write failed key={}", fullKey, e);
            return false;
        }
    }

    /** 写入缓存条目：将当前时间作为缓存时间一起存入 */
    public <T> boolean setEntry(String key, T value, Duration ttl) {
        return set(key, new CacheEntry<>(value, Instant.now()), ttl);
    }

    /** 批量删除缓存键 */
    public void delete(String... keys) {
        // 1. 校验开关与入参
        if (!enabled() || keys == null || keys.length == 0) {
            return;
        }
        try {
            // 2. 过滤空串并转换为带命名空间键数组
            String[] fullKeys = Arrays.stream(keys)
                    .filter(key -> key != null && !key.isBlank())
                    .map(this::namespaced)
                    .toArray(String[]::new);
            if (fullKeys.length == 0) {
                return;
            }
            // 3. 执行批量删除
            Long deleted = redisTemplate.delete(Arrays.asList(fullKeys));
            log.info("Redis cache invalidation keys={} deleted={}", Arrays.toString(fullKeys), deleted);
        } catch (Exception e) {
            log.warn("Redis cache invalidation failed keys={}", Arrays.toString(keys), e);
        }
    }

    /** 对指定键执行自增，并刷新过期时间，返回自增后的值 */
    public long increment(String key, Duration ttl) {
        // 1. 缓存未启用则返回0
        if (!enabled()) {
            return 0L;
        }
        // 2. 拼接完整键
        String fullKey = namespaced(key);
        try {
            // 3. 执行自增
            Long value = redisTemplate.opsForValue().increment(fullKey);
            // 4. 刷新过期时间
            if (value != null) {
                redisTemplate.expire(fullKey, ttl);
            }
            return value != null ? value : 0L;
        } catch (Exception e) {
            log.warn("Redis increment failed key={}", fullKey, e);
            return 0L;
        }
    }

    /** 获取用户的推荐版本号，不存在时初始化为1 */
    public long getUserRecommendationVersion(Integer userId) {
        // 1. 关闭缓存或无用户则返回0
        if (!enabled() || userId == null) {
            return 0L;
        }
        String key = "novel:rec:version:" + userId;
        try {
            // 2. 读取已有版本号
            String value = redisTemplate.opsForValue().get(namespaced(key));
            if (value != null) {
                return Long.parseLong(value);
            }
            // 3. 不存在则初始化为1并设置过期时间
            redisTemplate.opsForValue().set(namespaced(key), "1", cacheProperties.getVersionTtl());
            return 1L;
        } catch (Exception e) {
            log.warn("Redis recommendation-version read failed userId={}", userId, e);
            return 0L;
        }
    }

    /** 自增用户的推荐版本号，用于标记其推荐数据已变更 */
    public long incrementUserRecommendationVersion(Integer userId) {
        if (!enabled() || userId == null) {
            return 0L;
        }
        return increment("novel:rec:version:" + userId, cacheProperties.getVersionTtl());
    }

    /** 拼接键命名空间：已带前缀的键原样返回，否则补上前缀 */
    private String namespaced(String key) {
        return key != null && key.startsWith(KEY_PREFIX) ? key : KEY_PREFIX + key;
    }
}