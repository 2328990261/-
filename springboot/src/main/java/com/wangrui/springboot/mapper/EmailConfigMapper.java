package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.EmailConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 邮箱SMTP配置数据访问接口 */
@Mapper
public interface EmailConfigMapper {

    EmailConfig selectById(@Param("id") Integer id);

    int insert(EmailConfig config);

    int update(EmailConfig config);
}