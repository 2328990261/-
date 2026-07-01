package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserPreferenceTag;

import java.util.List;

public interface UserPreferenceService {
    // 获取用户偏好标签列表
    List<UserPreferenceTag> getUserPreferenceTags(Integer userId);

    // 根据类型获取用户偏好标签列表
    List<UserPreferenceTag> getUserPreferenceTagsByType(Integer userId, String tagType);

    /**
     * 根据用户收藏（及阅读情况）实时计算「收藏排序」标签，不读库不保存
     */
    List<UserPreferenceTag> getCollectionBasedTags(Integer userId);

    /**
     * 保存用户偏好标签（custom 写入库；collection 不持久化）。
     * @throws RuntimeException 数据库异常时抛出，由控制器转换为 Result
     */
    void saveUserPreferenceTags(Integer userId, String tagType, List<UserPreferenceTag> tags);

    // 删除用户偏好标签
    boolean deleteUserPreferenceTag(Integer id);

    // 删除用户所有偏好标签
    boolean deleteAllUserPreferenceTags(Integer userId);
}
