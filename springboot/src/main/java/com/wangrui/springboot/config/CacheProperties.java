package com.wangrui.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import org.springframework.boot.convert.DurationUnit;

import java.time.temporal.ChronoUnit;

@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    private boolean enabled = true;

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration recommendTtl = Duration.ofMinutes(5);

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration publicTtl = Duration.ofMinutes(5);

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration configTtl = Duration.ofMinutes(10);

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration tagHeatTtl = Duration.ofMinutes(5);

    @DurationUnit(ChronoUnit.DAYS)
    private Duration versionTtl = Duration.ofDays(7);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getRecommendTtl() {
        return recommendTtl;
    }

    public void setRecommendTtl(Duration recommendTtl) {
        this.recommendTtl = recommendTtl;
    }

    public Duration getPublicTtl() {
        return publicTtl;
    }

    public void setPublicTtl(Duration publicTtl) {
        this.publicTtl = publicTtl;
    }

    public Duration getConfigTtl() {
        return configTtl;
    }

    public void setConfigTtl(Duration configTtl) {
        this.configTtl = configTtl;
    }

    public Duration getTagHeatTtl() {
        return tagHeatTtl;
    }

    public void setTagHeatTtl(Duration tagHeatTtl) {
        this.tagHeatTtl = tagHeatTtl;
    }

    public Duration getVersionTtl() {
        return versionTtl;
    }

    public void setVersionTtl(Duration versionTtl) {
        this.versionTtl = versionTtl;
    }
}
