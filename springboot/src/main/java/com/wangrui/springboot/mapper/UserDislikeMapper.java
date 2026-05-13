package com.wangrui.springboot.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDislikeMapper {

    int insertNovel(@Param("userId") Integer userId, @Param("novelId") Integer novelId);

    int insertAuthor(@Param("userId") Integer userId, @Param("authorName") String authorName);

    int insertTag(@Param("userId") Integer userId, @Param("tagName") String tagName);

    List<Integer> selectNovelIdsByUserId(@Param("userId") Integer userId);

    List<String> selectAuthorsByUserId(@Param("userId") Integer userId);

    List<String> selectTagsByUserId(@Param("userId") Integer userId);
}
