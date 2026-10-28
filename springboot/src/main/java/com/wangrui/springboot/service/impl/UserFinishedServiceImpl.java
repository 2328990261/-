package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.pojo.UserFinishedNovel;
import com.wangrui.springboot.cache.RedisCacheService;
import com.wangrui.springboot.service.UserFinishedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 用户完读记录服务实现：标记/取消完读并失效推荐缓存 */
@Service
public class UserFinishedServiceImpl implements UserFinishedService {

    // 用户完读记录数据访问对象
    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;
    // Redis缓存服务
    @Autowired
    private RedisCacheService redisCacheService;

    /** 查询某用户的全部完读记录 */
    @Override
    public List<UserFinishedNovel> getUserFinishedNovels(Integer userId) {
        return userFinishedNovelMapper.selectByUserId(userId);
    }

    /** 校验用户是否已完读某小说 */
    @Override
    public boolean checkUserFinishedNovel(Integer userId, Integer novelId) {
        UserFinishedNovel finishedNovel = userFinishedNovelMapper.selectByUserIdAndNovelId(userId, novelId);
        return finishedNovel != null;
    }

    /** 标记小说为已完读，成功则使推荐缓存失效 */
    @Override
    public boolean markNovelAsFinished(Integer userId, Integer novelId) {
        try {
            // 1. 先检查是否已完读，已存在则直接返回成功
            UserFinishedNovel existingFinished = userFinishedNovelMapper.selectByUserIdAndNovelId(userId, novelId);
            if (existingFinished != null) {
                return true;
            }
            // 2. 创建新的完读记录并失效推荐缓存
            UserFinishedNovel finishedNovel = new UserFinishedNovel();
            finishedNovel.setUserId(userId);
            finishedNovel.setNovelId(novelId);
            userFinishedNovelMapper.insert(finishedNovel);
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 按记录ID取消完读标记 */
    @Override
    public boolean cancelFinishedNovel(Integer id) {
        try {
            // 1. 查询并删除完读记录
            UserFinishedNovel finishedNovel = userFinishedNovelMapper.selectById(id);
            userFinishedNovelMapper.deleteById(id);
            // 2. 使该作者用户的推荐缓存失效
            if (finishedNovel != null) {
                redisCacheService.incrementUserRecommendationVersion(finishedNovel.getUserId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 按用户与小说取消完读标记 */
    @Override
    public boolean cancelFinishedNovelByNovelId(Integer userId, Integer novelId) {
        try {
            // 1. 按用户与小说删除完读记录
            userFinishedNovelMapper.deleteByUserIdAndNovelId(userId, novelId);
            // 2. 使该作者用户的推荐缓存失效
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}