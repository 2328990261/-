package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.AiChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatSessionMapper {
    int insert(AiChatSession session);

    List<AiChatSession> selectByUserId(@Param("userId") Integer userId);

    AiChatSession selectById(@Param("id") Long id);

    int updateTitle(@Param("id") Long id, @Param("title") String title);

    int updatePinned(@Param("id") Long id, @Param("pinned") Integer pinned);

    int deleteById(@Param("id") Long id);

    int touch(@Param("id") Long id);
}