package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.AiAssistantConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAssistantConfigMapper {

    AiAssistantConfig selectById(@Param("id") Integer id);

    int insert(AiAssistantConfig config);

    int update(AiAssistantConfig config);
}