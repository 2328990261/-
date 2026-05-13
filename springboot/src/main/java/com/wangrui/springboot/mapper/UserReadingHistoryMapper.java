package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserReadingHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserReadingHistoryMapper {
    // 获取用户阅读历史列表
    List<UserReadingHistory> selectByUserId(@Param("userId") Integer userId);

    // 获取用户指定小说的阅读历史
    UserReadingHistory selectByUserIdAndNovelId(@Param("userId") Integer userId, @Param("novelId") Integer novelId);

    // 保存用户阅读历史
    int insert(UserReadingHistory userReadingHistory);

    // 更新用户阅读历史
    int update(UserReadingHistory userReadingHistory);

    // 删除用户阅读历史
    int deleteById(@Param("id") Integer id);

    // 删除用户指定小说的阅读历史
    int deleteByUserIdAndNovelId(@Param("userId") Integer userId, @Param("novelId") Integer novelId);

    /** 统计用户所有小说的阅读总时长（秒） */
    Long selectTotalReadDurationByUserId(@Param("userId") Integer userId);

    /** 全站阅读历史（用于标签热度聚合，数据量大时建议后续改定时任务落表） */
    List<UserReadingHistory> selectAllForSiteHeat();
}
