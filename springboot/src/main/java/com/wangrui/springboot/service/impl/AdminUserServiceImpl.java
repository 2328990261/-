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

/**
 * 管理端用户维护服务实现
 * 负责删除用户及其全部关联数据的级联清理
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    // 用户评论数据访问对象
    @Autowired
    private UserCommentMapper userCommentMapper;
    // 用户阅读历史数据访问对象
    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;
    // 用户完读记录数据访问对象
    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;
    // 用户主表数据访问对象
    @Autowired
    private UserMapper userMapper;
    // 用户偏好标签数据访问对象
    @Autowired
    private UserPreferenceTagMapper userPreferenceTagMapper;
    // 用户标签权重数据访问对象
    @Autowired
    private UserTagWeightMapper userTagWeightMapper;
    // 用户推荐画像数据访问对象
    @Autowired
    private UserRecommendProfileMapper userRecommendProfileMapper;
    // 用户不感兴趣数据访问对象
    @Autowired
    private UserDislikeMapper userDislikeMapper;

    /**
     * 删除用户及其评论、阅读、完读、收藏、偏好、推荐画像、不感兴趣等关联数据
     * 整个方法在一个事务中执行，任一步失败则整体回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserAndRelations(Integer userId) {
        // 0. 用户ID为空则直接结束
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