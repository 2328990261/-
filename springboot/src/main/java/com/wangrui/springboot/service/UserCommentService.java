package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserComment;

import java.util.List;

public interface UserCommentService {
    // 获取用户评论列表
    List<UserComment> getUserComments(Integer userId);

    // 获取小说的评论列表
    List<UserComment> getNovelComments(Integer novelId);

    // 获取指定评论
    UserComment getCommentById(Integer id);

    // 发表评论
    boolean addComment(UserComment comment);

    // 更新评论
    boolean updateComment(UserComment comment);

    // 删除评论
    boolean deleteComment(Integer id);
}
