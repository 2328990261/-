package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserCommentMapper {
    // 获取用户评论列表
    List<UserComment> selectByUserId(@Param("userId") Integer userId);

    // 获取小说的评论列表
    List<UserComment> selectByNovelId(@Param("novelId") Integer novelId);

    // 获取指定评论
    UserComment selectById(@Param("id") Integer id);

    // 保存用户评论
    int insert(UserComment userComment);

    // 更新用户评论
    int update(UserComment userComment);

    // 删除用户评论
    int deleteById(@Param("id") Integer id);
}
