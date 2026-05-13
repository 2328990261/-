package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.mapper.UserDislikeMapper;
import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.mapper.UserPreferenceTagMapper;
import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.mapper.UserRecommendProfileMapper;
import com.wangrui.springboot.mapper.UserTagWeightMapper;
import com.wangrui.springboot.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserCommentMapper userCommentMapper;
    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;
    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserPreferenceTagMapper userPreferenceTagMapper;
    @Autowired
    private UserTagWeightMapper userTagWeightMapper;
    @Autowired
    private UserRecommendProfileMapper userRecommendProfileMapper;
    @Autowired
    private UserDislikeMapper userDislikeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserAndRelations(Integer userId) {
        if (userId == null) {
            return;
        }
        userCommentMapper.deleteByUserId(userId);
        userReadingHistoryMapper.deleteByUserId(userId);
        userFinishedNovelMapper.deleteByUserId(userId);
        userMapper.deleteAllCollectionsByUserId(userId);
        userPreferenceTagMapper.deleteByUserId(userId);
        userTagWeightMapper.deleteByUserId(userId);
        userRecommendProfileMapper.deleteByUserId(userId);
        userDislikeMapper.deleteDislikeNovelsByUserId(userId);
        userDislikeMapper.deleteDislikeAuthorsByUserId(userId);
        userDislikeMapper.deleteDislikeTagsByUserId(userId);
        userMapper.deleteUserById(userId);
    }
}
