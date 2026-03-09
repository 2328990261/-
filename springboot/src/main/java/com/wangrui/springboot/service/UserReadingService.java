package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserReadingHistory;

import java.util.List;

public interface UserReadingService {
    // 获取用户阅读历史列表
    List<UserReadingHistory> getUserReadingHistory(Integer userId);

    // 获取用户指定小说的阅读历史
    UserReadingHistory getUserReadingHistoryByNovelId(Integer userId, Integer novelId);

    // 保存或更新用户阅读历史
    boolean saveOrUpdateUserReadingHistory(UserReadingHistory history);

    // 删除用户阅读历史
    boolean deleteUserReadingHistory(Integer id);

    // 删除用户指定小说的阅读历史
    boolean deleteUserReadingHistoryByNovelId(Integer userId, Integer novelId);

    /** 获取用户所有小说的阅读总时长（秒） */
    long getTotalReadDurationSeconds(Integer userId);
}
