package com.wangrui.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表 user_recommend_profile：每用户推荐配置（Item-CF 混合开关、MMR、解释开关 + 行为权重 + 阈值参数）。
 */
public class UserRecommendProfile {
    private Integer id;
    private Integer userId;

    private Integer cfEnabled;
    private Integer explainEnabled;
    /** 是否对推荐结果做 MMR 多样性重排：0 否 1 是 */
    private Integer mmrEnabled;

    private BigDecimal wCollection;
    private BigDecimal wFinished;
    private BigDecimal wReadProgress;
    private BigDecimal wReadDuration;
    private BigDecimal wComment;

    private Integer coldStartCollectionThreshold;
    private Integer recentReadLimit;
    private Integer topkSimilarPerSeed;
    private Integer candidateLimit;
    private Integer recommendLimit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCfEnabled() {
        return cfEnabled;
    }

    public void setCfEnabled(Integer cfEnabled) {
        this.cfEnabled = cfEnabled;
    }

    public Integer getExplainEnabled() {
        return explainEnabled;
    }

    public void setExplainEnabled(Integer explainEnabled) {
        this.explainEnabled = explainEnabled;
    }

    public Integer getMmrEnabled() {
        return mmrEnabled;
    }

    public void setMmrEnabled(Integer mmrEnabled) {
        this.mmrEnabled = mmrEnabled;
    }

    public BigDecimal getwCollection() {
        return wCollection;
    }

    public void setwCollection(BigDecimal wCollection) {
        this.wCollection = wCollection;
    }

    public BigDecimal getwFinished() {
        return wFinished;
    }

    public void setwFinished(BigDecimal wFinished) {
        this.wFinished = wFinished;
    }

    public BigDecimal getwReadProgress() {
        return wReadProgress;
    }

    public void setwReadProgress(BigDecimal wReadProgress) {
        this.wReadProgress = wReadProgress;
    }

    public BigDecimal getwReadDuration() {
        return wReadDuration;
    }

    public void setwReadDuration(BigDecimal wReadDuration) {
        this.wReadDuration = wReadDuration;
    }

    public BigDecimal getwComment() {
        return wComment;
    }

    public void setwComment(BigDecimal wComment) {
        this.wComment = wComment;
    }

    public Integer getColdStartCollectionThreshold() {
        return coldStartCollectionThreshold;
    }

    public void setColdStartCollectionThreshold(Integer coldStartCollectionThreshold) {
        this.coldStartCollectionThreshold = coldStartCollectionThreshold;
    }

    public Integer getRecentReadLimit() {
        return recentReadLimit;
    }

    public void setRecentReadLimit(Integer recentReadLimit) {
        this.recentReadLimit = recentReadLimit;
    }

    public Integer getTopkSimilarPerSeed() {
        return topkSimilarPerSeed;
    }

    public void setTopkSimilarPerSeed(Integer topkSimilarPerSeed) {
        this.topkSimilarPerSeed = topkSimilarPerSeed;
    }

    public Integer getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(Integer candidateLimit) {
        this.candidateLimit = candidateLimit;
    }

    public Integer getRecommendLimit() {
        return recommendLimit;
    }

    public void setRecommendLimit(Integer recommendLimit) {
        this.recommendLimit = recommendLimit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static UserRecommendProfile defaultForUser(Integer userId) {
        UserRecommendProfile p = new UserRecommendProfile();
        p.setUserId(userId);
        p.setCfEnabled(1);
        p.setExplainEnabled(0);
        p.setMmrEnabled(1);
        p.setwCollection(new BigDecimal("1.000000"));
        p.setwFinished(new BigDecimal("1.000000"));
        p.setwReadProgress(new BigDecimal("0.600000"));
        p.setwReadDuration(new BigDecimal("0.400000"));
        p.setwComment(new BigDecimal("0.700000"));
        p.setColdStartCollectionThreshold(3);
        p.setRecentReadLimit(30);
        p.setTopkSimilarPerSeed(50);
        p.setCandidateLimit(200);
        p.setRecommendLimit(20);
        return p;
    }
}

