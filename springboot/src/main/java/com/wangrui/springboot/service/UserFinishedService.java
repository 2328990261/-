package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserFinishedNovel;

import java.util.List;

public interface UserFinishedService {
    // 获取用户完读记录列表
    List<UserFinishedNovel> getUserFinishedNovels(Integer userId);

    // 检查用户是否已完读指定小说
    boolean checkUserFinishedNovel(Integer userId, Integer novelId);

    // 标记小说为完读
    boolean markNovelAsFinished(Integer userId, Integer novelId);

    // 取消完读记录
    boolean cancelFinishedNovel(Integer id);

    // 取消指定小说的完读记录
    boolean cancelFinishedNovelByNovelId(Integer userId, Integer novelId);
}
