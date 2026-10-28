package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserDislikeRequest;
import com.wangrui.springboot.pojo.UserDislikeSnapshot;

/** 用户不感兴趣服务接口：屏蔽小说/作者、标签降权与屏蔽列表查询 */
public interface UserDislikeService {

    boolean saveDislike(Integer userId, UserDislikeRequest request);

    UserDislikeSnapshot getDislike(Integer userId);
}