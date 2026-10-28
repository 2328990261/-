package com.wangrui.springboot.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 用户不感兴趣数据访问接口，负责屏蔽小说、作者与标签的写入和查询 */
public interface UserDislikeMapper {

    int insertNovel(@Param("userId") Integer userId, @Param("novelId") Integer novelId);

    int insertAuthor(@Param("userId") Integer userId, @Param("authorName") String authorName);

    int insertTag(@Param("userId") Integer userId, @Param("tagName") String tagName);

    List<Integer> selectNovelIdsByUserId(@Param("userId") Integer userId);

    List<String> selectAuthorsByUserId(@Param("userId") Integer userId);

    List<String> selectTagsByUserId(@Param("userId") Integer userId);

    int deleteDislikeNovelsByUserId(@Param("userId") Integer userId);

    int deleteDislikeAuthorsByUserId(@Param("userId") Integer userId);

    int deleteDislikeTagsByUserId(@Param("userId") Integer userId);
}