package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.CustomerServiceMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CustomerServiceMessageMapper {
    int insert(CustomerServiceMessage message);

    List<CustomerServiceMessage> selectByUserId(@Param("userId") Integer userId);

    List<Map<String, Object>> selectConversations();

    int markUserMessagesRead(@Param("userId") Integer userId);

    int markServiceMessagesRead(@Param("userId") Integer userId);
}
