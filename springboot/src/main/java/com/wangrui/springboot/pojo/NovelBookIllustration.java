package com.wangrui.springboot.pojo;

import java.util.Date;

/**
 * 分卷插画：EPUB 正文中提取的图片，落盘在上传目录 epub-images 下，表内记录访问 URL 与顺序。
 */
public class NovelBookIllustration {
  private Integer id;
  private Integer volumeId;
  private Integer sortOrder;
  private String url;
  private String originalName;
  private Date createdAt;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Integer getVolumeId() { return volumeId; }
  public void setVolumeId(Integer volumeId) { this.volumeId = volumeId; }

  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }

  public String getOriginalName() { return originalName; }
  public void setOriginalName(String originalName) { this.originalName = originalName; }

  public Date getCreatedAt() { return createdAt; }
  public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
