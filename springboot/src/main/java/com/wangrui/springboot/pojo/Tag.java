package com.wangrui.springboot.pojo;

import java.time.LocalDateTime;

/**
 * 平台标签表，用于管理员填删改查与推荐权重配置
 */
public class Tag {
    private Integer id;
    private String name;
    private java.math.BigDecimal recommendWeight;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public java.math.BigDecimal getRecommendWeight() { return recommendWeight; }
    public void setRecommendWeight(java.math.BigDecimal recommendWeight) { this.recommendWeight = recommendWeight; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
