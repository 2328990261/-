package com.wangrui.springboot.pojo;

import java.util.Date;

public class AiChatMessage {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String thinking;
    private String imageUrl;
    private String booksJson;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getThinking() { return thinking; }
    public void setThinking(String thinking) { this.thinking = thinking; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getBooksJson() { return booksJson; }
    public void setBooksJson(String booksJson) { this.booksJson = booksJson; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}