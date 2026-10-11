package com.wangrui.springboot.pojo;

import java.util.Date;

public class AiChatSession {
    private Long id;
    private Integer userId;
    private String title;
    private Integer pinned; // 是否置顶：1=置顶，0=否
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getPinned() { return pinned; }
    public void setPinned(Integer pinned) { this.pinned = pinned; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}