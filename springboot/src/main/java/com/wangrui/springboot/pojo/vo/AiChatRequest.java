package com.wangrui.springboot.pojo.vo;

/** AI聊天请求体：会话ID + 用户输入内容 + 可选的图片URL */
public class AiChatRequest {
    private Long sessionId;
    private String content;
    private String imageUrl;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}