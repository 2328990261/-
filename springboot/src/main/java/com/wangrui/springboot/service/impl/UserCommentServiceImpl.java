package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.pojo.UserComment;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.UserCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCommentServiceImpl implements UserCommentService {

    @Autowired
    private UserCommentMapper userCommentMapper;
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public List<UserComment> getUserComments(Integer userId) {
        return userCommentMapper.selectByUserId(userId);
    }

    @Override
    public List<UserComment> getNovelComments(Integer novelId) {
        return userCommentMapper.selectByNovelId(novelId);
    }

    @Override
    public UserComment getCommentById(Integer id) {
        return userCommentMapper.selectById(id);
    }

    @Override
    public boolean addComment(UserComment comment) {
        try {
            userCommentMapper.insert(comment);
            redisCacheService.incrementUserRecommendationVersion(comment.getUserId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateComment(UserComment comment) {
        try {
            userCommentMapper.update(comment);
            redisCacheService.incrementUserRecommendationVersion(comment.getUserId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteComment(Integer id) {
        try {
            UserComment comment = userCommentMapper.selectById(id);
            userCommentMapper.deleteById(id);
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
