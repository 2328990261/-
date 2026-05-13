package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserDislikeRequest;
import com.wangrui.springboot.pojo.UserDislikeSnapshot;

public interface UserDislikeService {

    boolean saveDislike(Integer userId, UserDislikeRequest request);

    UserDislikeSnapshot getDislike(Integer userId);
}
