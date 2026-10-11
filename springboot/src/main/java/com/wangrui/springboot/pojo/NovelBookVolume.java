package com.wangrui.springboot.pojo;

import java.util.Date;
import java.util.List;

// 对应数据库novel_book_volume表
public class NovelBookVolume {
  private Integer id;
  private Integer mainBookId; // 对应数据库main_book_id
  private String volumeUuid; // 分卷业务唯一标识（插画目录名、公开引用）
  private String volumeName;
  private String content; // 分卷内容
  private Date createTime; // 创建时间
  private List<NovelBookIllustration> illustrations; // 本卷插画（阅读页渲染用，非表字段）

  // 无参构造
  public NovelBookVolume() {}

  // Getter+Setter
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Integer getMainBookId() { return mainBookId; }
  public void setMainBookId(Integer mainBookId) { this.mainBookId = mainBookId; }

  public String getVolumeUuid() { return volumeUuid; }
  public void setVolumeUuid(String volumeUuid) { this.volumeUuid = volumeUuid; }

  public String getVolumeName() { return volumeName; }
  public void setVolumeName(String volumeName) { this.volumeName = volumeName; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public Date getCreateTime() { return createTime; }
  public void setCreateTime(Date createTime) { this.createTime = createTime; }

  public List<NovelBookIllustration> getIllustrations() { return illustrations; }
  public void setIllustrations(List<NovelBookIllustration> illustrations) { this.illustrations = illustrations; }
}
