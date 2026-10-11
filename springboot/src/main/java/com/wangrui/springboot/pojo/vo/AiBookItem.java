package com.wangrui.springboot.pojo.vo;

public class AiBookItem {
    private Integer novelId;
    private String title;
    private String author;
    private String tags;
    private String cover;
    private String reason;

    public Integer getNovelId() { return novelId; }
    public void setNovelId(Integer novelId) { this.novelId = novelId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}