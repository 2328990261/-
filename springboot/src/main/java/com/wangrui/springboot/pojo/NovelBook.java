package com.wangrui.springboot.pojo;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
// 对应数据库novel_book_main表
public class NovelBook {
  private Integer id;
  private String bookMainName; // 对应数据库book_main_name
  private String author;
  private String label;
  private String cover; // 封面文件名或相对路径
  private Integer readCount;
  private Integer status; // 0=下架 1=上架
  private Date createTime;
  private List<NovelBookVolume> volumeList;

  // 无参构造
  public NovelBook() {}

  // Getter+Setter（保留原有字段的方法，新增readCount的get/set）
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public String getBookMainName() { return bookMainName; }
  public void setBookMainName(String bookMainName) { this.bookMainName = bookMainName; }

  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }

  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }

  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }

  // 新增：readCount的Getter+Setter
  public Integer getReadCount() { return readCount; }
  public void setReadCount(Integer readCount) { this.readCount = readCount; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }

  public Date getCreateTime() { return createTime; }
  public void setCreateTime(Date createTime) { this.createTime = createTime; }

  // 务必保证方法名、参数类型完全一致
  public void setVolumeList(List<NovelBookVolume> volumeList) {
    this.volumeList = volumeList;
  }

  // 对应的get方法也要有
  public List<NovelBookVolume> getVolumeList() {
    return volumeList;
  }

}