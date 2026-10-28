package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.pojo.UserComment;
import com.wangrui.springboot.cache.RedisCacheService;
import com.wangrui.springboot.service.UserCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 用户评论服务实现：评论的增删改并失效推荐缓存 */
@Service
public class UserCommentServiceImpl implements UserCommentService {

    // 用户评论数据访问对象
    @Autowired
    private UserCommentMapper userCommentMapper;
    // Redis缓存服务
    @Autowired
    private RedisCacheService redisCacheService;

    /** 查询某用户的所有评论 */
    @Override
    public List<UserComment> getUserComments(Integer userId) {
        return userCommentMapper.selectByUserId(userId);
    }

    /** 查询某小说的全部评论 */
    @Override
    public List<UserComment> getNovelComments(Integer novelId) {
        return userCommentMapper.selectByNovelId(novelId);
    }

    /** 按ID查询评论 */
    @Override
    public UserComment getCommentById(Integer id) {
        return userCommentMapper.selectById(id);
    }

    /** 新增评论，成功则使该用户推荐缓存失效 */
    @Override
    public boolean addComment(UserComment comment) {
        try {
            // 1. 插入评论
            userCommentMapper.insert(comment);
            // 2. 使该用户的推荐结果缓存失效
            redisCacheService.incrementUserRecommendationVersion(comment.getUserId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 更新评论，成功则使该用户推荐缓存失效 */
    @Override
    public boolean updateComment(UserComment comment) {
        try {
            // 1. 更新评论
            userCommentMapper.update(comment);
            // 2. 使该用户的推荐结果缓存失效
            redisCacheService.incrementUserRecommendationVersion(comment.getUserId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 删除评论，成功则使该用户推荐缓存失效 */
    @Override
    public boolean deleteComment(Integer id) {
        try {
            // 1. 查询并删除评论
            UserComment comment = userCommentMapper.selectById(id);
            userCommentMapper.deleteById(id);
            // 2. 使该作者用户的推荐结果缓存失效
            if (comment != null) {
                redisCacheService.incrementUserRecommendationVersion(comment.getUserId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}