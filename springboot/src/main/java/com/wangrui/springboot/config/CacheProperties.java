package com.wangrui.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import org.springframework.boot.convert.DurationUnit;

import java.time.temporal.ChronoUnit;

/**
 * 缓存配置属性
 * 通过 application.yml 中 app.cache 前缀配置各项缓存的过期时间与总开关
 */
@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    // 缓存总开关，默认开启
    private boolean enabled = true;

    // 推荐结果缓存过期时间，默认5分钟
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration recommendTtl = Duration.ofMinutes(5);

    // 公开数据（如小说详情）缓存过期时间，默认5分钟
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration publicTtl = Duration.ofMinutes(5);

    // 配置类缓存过期时间，默认10分钟
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration configTtl = Duration.ofMinutes(10);

    // 标签热度缓存过期时间，默认5分钟
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration tagHeatTtl = Duration.ofMinutes(5);

    // 版本号缓存过期时间，默认7天
    @DurationUnit(ChronoUnit.DAYS)
    private Duration versionTtl = Duration.ofDays(7);

    /** 是否启用缓存 */
    public boolean isEnabled() {
        return enabled;
    }

    /** 设置缓存开关 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 获取推荐结果缓存有效期 */
    public Duration getRecommendTtl() {
        return recommendTtl;
    }

    /** 设置推荐结果缓存有效期 */
    public void setRecommendTtl(Duration recommendTtl) {
        this.recommendTtl = recommendTtl;
    }

    /** 获取公开数据缓存有效期 */
    public Duration getPublicTtl() {
        return publicTtl;
    }

    /** 设置公开数据缓存有效期 */
    public void setPublicTtl(Duration publicTtl) {
        this.publicTtl = publicTtl;
    }

    /** 获取配置类缓存有效期 */
    public Duration getConfigTtl() {
        return configTtl;
    }

    /** 设置配置类缓存有效期 */
    public void setConfigTtl(Duration configTtl) {
        this.configTtl = configTtl;
    }

    /** 获取标签热度缓存有效期 */
    public Duration getTagHeatTtl() {
        return tagHeatTtl;
    }

    /** 设置标签热度缓存有效期 */
    public void setTagHeatTtl(Duration tagHeatTtl) {
        this.tagHeatTtl = tagHeatTtl;
    }

    /** 获取版本号缓存有效期 */
    public Duration getVersionTtl() {
        return versionTtl;
    }

    /** 设置版本号缓存有效期 */
    public void setVersionTtl(Duration versionTtl) {
        this.versionTtl = versionTtl;
    }
}