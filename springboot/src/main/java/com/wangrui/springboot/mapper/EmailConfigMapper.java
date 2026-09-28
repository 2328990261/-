package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.EmailConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailConfigMapper {

    EmailConfig selectById(@Param("id") Integer id);

    int insert(EmailConfig config);

    int update(EmailConfig config);
}
