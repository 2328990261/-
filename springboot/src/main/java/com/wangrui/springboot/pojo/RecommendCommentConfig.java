package com.wangrui.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表 recommend_comment_config：推荐行为权重（评论 user_comment + 阅读历史 user_reading_history）。
 * 收藏不参与加权（推荐主流程已基于收藏衍生标签）。
 */
public class RecommendCommentConfig {
    private Integer id;
    /** 评论热度是否参与 */
    private Integer enabled;
    private BigDecimal commentWeight;
    private Integer commentCountCap;
    /** 阅读历史是否参与 */
    private Integer readHistoryEnabled;
    private BigDecimal readHistoryWeight;
    /** 仅取用户最近 N 条阅读记录（按 last_read_time） */
    private Integer readHistoryRecentLimit;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getCommentWeight() {
        return commentWeight;
    }

    public void setCommentWeight(BigDecimal commentWeight) {
        this.commentWeight = commentWeight;
    }

    public Integer getCommentCountCap() {
        return commentCountCap;
    }

    public void setCommentCountCap(Integer commentCountCap) {
        this.commentCountCap = commentCountCap;
    }

    public Integer getReadHistoryEnabled() {
        return readHistoryEnabled;
    }

    public void setReadHistoryEnabled(Integer readHistoryEnabled) {
        this.readHistoryEnabled = readHistoryEnabled;
    }

    public BigDecimal getReadHistoryWeight() {
        return readHistoryWeight;
    }

    public void setReadHistoryWeight(BigDecimal readHistoryWeight) {
        this.readHistoryWeight = readHistoryWeight;
    }

    public Integer getReadHistoryRecentLimit() {
        return readHistoryRecentLimit;
    }

    public void setReadHistoryRecentLimit(Integer readHistoryRecentLimit) {
        this.readHistoryRecentLimit = readHistoryRecentLimit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
