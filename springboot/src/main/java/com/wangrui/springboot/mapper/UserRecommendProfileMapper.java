package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserRecommendProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户推荐画像数据访问接口，负责读取与更新用户的推荐配置画像 */
@Mapper
public interface UserRecommendProfileMapper {
    UserRecommendProfile selectByUserId(@Param("userId") Integer userId);

    int insert(UserRecommendProfile profile);

    int update(UserRecommendProfile profile);

    int deleteByUserId(@Param("userId") Integer userId);
}