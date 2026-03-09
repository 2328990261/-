package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.service.UserReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserReadingServiceImpl implements UserReadingService {

    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;

    @Override
    public List<UserReadingHistory> getUserReadingHistory(Integer userId) {
        return userReadingHistoryMapper.selectByUserId(userId);
    }

    @Override
    public UserReadingHistory getUserReadingHistoryByNovelId(Integer userId, Integer novelId) {
        return userReadingHistoryMapper.selectByUserIdAndNovelId(userId, novelId);
    }

    @Override
    public boolean saveOrUpdateUserReadingHistory(UserReadingHistory history) {
        try {
            // 先查询是否已存在阅读历史
            UserReadingHistory existingHistory = userReadingHistoryMapper.selectByUserIdAndNovelId(history.getUserId(), history.getNovelId());
            
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteUserReadingHistory(Integer id) {
        try {
            userReadingHistoryMapper.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteUserReadingHistoryByNovelId(Integer userId, Integer novelId) {
        try {
            userReadingHistoryMapper.deleteByUserIdAndNovelId(userId, novelId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long getTotalReadDurationSeconds(Integer userId) {
        Long total = userReadingHistoryMapper.selectTotalReadDurationByUserId(userId);
        return total != null ? total : 0L;
    }
}
