package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.cache.RedisCacheService;
import com.wangrui.springboot.service.UserReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 用户阅读历史服务实现：记录阅读进度与时长，并失效推荐缓存 */
@Service
public class UserReadingServiceImpl implements UserReadingService {

    // 用户阅读历史数据访问对象
    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;
    // Redis缓存服务
    @Autowired
    private RedisCacheService redisCacheService;

    /** 查询某用户的全部阅读历史 */
    @Override
    public List<UserReadingHistory> getUserReadingHistory(Integer userId) {
        return userReadingHistoryMapper.selectByUserId(userId);
    }

    /** 查询某用户对某小说的阅读历史 */
    @Override
    public UserReadingHistory getUserReadingHistoryByNovelId(Integer userId, Integer novelId) {
        return userReadingHistoryMapper.selectByUserIdAndNovelId(userId, novelId);
    }

    /** 保存或更新阅读历史，并失效推荐缓存 */
    @Override
    public boolean saveOrUpdateUserReadingHistory(UserReadingHistory history) {
        try {
            // 1. 先查询是否已存在阅读历史
            UserReadingHistory existingHistory = userReadingHistoryMapper.selectByUserIdAndNovelId(history.getUserId(), history.getNovelId());
            // 2. 已存在则更新，否则新增
            if (existingHistory != null) {
                // 更新已存在的阅读历史
                existingHistory.setChapterId(history.getChapterId());
                existingHistory.setReadDuration(history.getReadDuration());
                existingHistory.setReadProgress(history.getReadProgress());
                userReadingHistoryMapper.update(existingHistory);
            } else {
                // 保存新的阅读历史
                userReadingHistoryMapper.insert(history);
            }
            // 3. 失效该用户的推荐缓存
            redisCacheService.incrementUserRecommendationVersion(history.getUserId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 按记录ID删除阅读历史 */
    @Override
    public boolean deleteUserReadingHistory(Integer id) {
        try {
            // 1. 查询并删除阅读历史
            UserReadingHistory history = userReadingHistoryMapper.selectById(id);
            userReadingHistoryMapper.deleteById(id);
            // 2. 失效该作者用户的推荐缓存
            if (history != null) {
                redisCacheService.incrementUserRecommendationVersion(history.getUserId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 按用户与小说删除阅读历史 */
    @Override
    public boolean deleteUserReadingHistoryByNovelId(Integer userId, Integer novelId) {
        try {
            // 1. 按用户与小说删除记录
            userReadingHistoryMapper.deleteByUserIdAndNovelId(userId, novelId);
            // 2. 失效该作者用户的推荐缓存
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 统计某用户累计阅读时长（秒） */
    @Override
    public long getTotalReadDurationSeconds(Integer userId) {
        Long total = userReadingHistoryMapper.selectTotalReadDurationByUserId(userId);
        return total != null ? total : 0L;
    }
}