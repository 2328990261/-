package com.wangrui.springboot.pojo;

import java.util.Date;

// 对应数据库novel_book_volume表
public class NovelBookVolume {
  private Integer id;
  private Integer mainBookId; // 对应数据库main_book_id
  private String volumeName;
  private String content; // 分卷内容
  private Date createTime; // 创建时间

  // 无参构造
  public NovelBookVolume() {}

  // Getter+Setter
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Integer getMainBookId() { return mainBookId; }
  public void setMainBookId(Integer mainBookId) { this.mainBookId = mainBookId; }

  public String getVolumeName() { return volumeName; }
  public void setVolumeName(String volumeName) { this.volumeName = volumeName; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public Date getCreateTime() { return createTime; }
  public void setCreateTime(Date createTime) { this.createTime = createTime; }
}