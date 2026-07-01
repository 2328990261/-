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
        // 1. 删除评论
        userCommentMapper.deleteByUserId(userId);
        // 2. 删除阅读历史
        userReadingHistoryMapper.deleteByUserId(userId);
        // 3. 删除完读记录
        userFinishedNovelMapper.deleteByUserId(userId);
        // 4. 删除收藏
        userMapper.deleteAllCollectionsByUserId(userId);
        // 5. 删除偏好标签
        userPreferenceTagMapper.deleteByUserId(userId);
        // 6. 删除标签权重
        userTagWeightMapper.deleteByUserId(userId);
        // 7. 删除推荐配置
        userRecommendProfileMapper.deleteByUserId(userId);
        // 8. 删除不喜欢的轻小说、作者、标签
        userDislikeMapper.deleteDislikeNovelsByUserId(userId);
        userDislikeMapper.deleteDislikeAuthorsByUserId(userId);
        userDislikeMapper.deleteDislikeTagsByUserId(userId);
        // 9. 删除用户
        userMapper.deleteUserById(userId);
    }
}
