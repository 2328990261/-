package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.AiAssistantConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** AI助手配置数据访问接口 */
@Mapper
public interface AiAssistantConfigMapper {

    AiAssistantConfig selectById(@Param("id") Integer id);

    int insert(AiAssistantConfig config);

    int update(AiAssistantConfig config);
}