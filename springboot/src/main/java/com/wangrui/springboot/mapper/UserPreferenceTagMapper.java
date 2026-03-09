package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserPreferenceTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserPreferenceTagMapper {
    // 获取用户偏好标签列表
    List<UserPreferenceTag> selectByUserId(@Param("userId") Integer userId);

    // 根据类型获取用户偏好标签列表
    List<UserPreferenceTag> selectByUserIdAndTagType(@Param("userId") Integer userId, @Param("tagType") String tagType);

    // 保存用户偏好标签
    int insert(UserPreferenceTag userPreferenceTag);

    // 批量保存用户偏好标签
    int insertBatch(@Param("tags") List<UserPreferenceTag> tags);

    // 删除用户所有偏好标签
    int deleteByUserId(@Param("userId") Integer userId);
    
    // 删除用户指定类型的偏好标签
    int deleteByUserIdAndTagType(@Param("userId") Integer userId, @Param("tagType") String tagType);

    // 删除指定偏好标签
    int deleteById(@Param("id") Integer id);

    // 更新用户偏好标签
    int update(UserPreferenceTag userPreferenceTag);
}
