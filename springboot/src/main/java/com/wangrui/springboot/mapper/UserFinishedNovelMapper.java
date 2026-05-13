package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserFinishedNovel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserFinishedNovelMapper {
    // 获取用户完读记录列表
    List<UserFinishedNovel> selectByUserId(@Param("userId") Integer userId);

    // 检查用户是否已完读指定小说
    UserFinishedNovel selectByUserIdAndNovelId(@Param("userId") Integer userId, @Param("novelId") Integer novelId);

    // 保存用户完读记录
    int insert(UserFinishedNovel userFinishedNovel);

    // 删除用户完读记录
    int deleteById(@Param("id") Integer id);

    // 删除用户指定小说的完读记录
    int deleteByUserIdAndNovelId(@Param("userId") Integer userId, @Param("novelId") Integer novelId);

    /** 全站完读过的小说 id（去重） */
    List<Integer> selectAllFinishedDistinctNovelIds();
}
