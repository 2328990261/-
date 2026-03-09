package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.pojo.UserComment;
import com.wangrui.springboot.service.UserCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCommentServiceImpl implements UserCommentService {

    @Autowired
    private UserCommentMapper userCommentMapper;

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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteComment(Integer id) {
        try {
            userCommentMapper.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
