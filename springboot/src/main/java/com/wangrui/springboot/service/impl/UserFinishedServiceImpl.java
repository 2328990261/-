package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.pojo.UserFinishedNovel;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.UserFinishedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFinishedServiceImpl implements UserFinishedService {

    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public List<UserFinishedNovel> getUserFinishedNovels(Integer userId) {
        return userFinishedNovelMapper.selectByUserId(userId);
    }

    @Override
    public boolean checkUserFinishedNovel(Integer userId, Integer novelId) {
        UserFinishedNovel finishedNovel = userFinishedNovelMapper.selectByUserIdAndNovelId(userId, novelId);
        return finishedNovel != null;
    }

    @Override
    public boolean markNovelAsFinished(Integer userId, Integer novelId) {
        try {
            // 先检查是否已完读
            UserFinishedNovel existingFinished = userFinishedNovelMapper.selectByUserIdAndNovelId(userId, novelId);
            if (existingFinished != null) {
                return true; // 已经标记为完读，直接返回成功
            }

            // 创建新的完读记录
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

    @Override
    public boolean cancelFinishedNovel(Integer id) {
        try {
            UserFinishedNovel finishedNovel = userFinishedNovelMapper.selectById(id);
            userFinishedNovelMapper.deleteById(id);
            if (finishedNovel != null) {
                redisCacheService.incrementUserRecommendationVersion(finishedNovel.getUserId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean cancelFinishedNovelByNovelId(Integer userId, Integer novelId) {
        try {
            userFinishedNovelMapper.deleteByUserIdAndNovelId(userId, novelId);
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
