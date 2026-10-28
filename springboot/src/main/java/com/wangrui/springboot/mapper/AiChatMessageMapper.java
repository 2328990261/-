package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** AI助手聊天消息数据访问接口，负责消息的写入与按会话查询 */
@Mapper
public interface AiChatMessageMapper {
    int insert(AiChatMessage message);

    List<AiChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 删除指定会话下的全部消息（删除会话时级联清理）
     * @param sessionId 会话ID
     * @return 受影响行数
     */
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}