package com.wangrui.springboot.service;

/**
 * 管理端用户维护：级联删除等。
 */
public interface AdminUserService {

    /**
     * 删除用户及其评论、阅读、完读、收藏、偏好、推荐画像、不感兴趣等关联数据。
     */
    void deleteUserAndRelations(Integer userId);
}
