package com.wangrui.springboot.pojo;


/** 用户收藏实体类，映射数据库 user_collection 表 */
public class UserCollection {

  private long id;
  private long userId;
  private long novelId;
  private java.sql.Timestamp createdAt;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }


  public long getNovelId() {
    return novelId;
  }

  public void setNovelId(long novelId) {
    this.novelId = novelId;
  }


  public java.sql.Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(java.sql.Timestamp createdAt) {
    this.createdAt = createdAt;
  }

}