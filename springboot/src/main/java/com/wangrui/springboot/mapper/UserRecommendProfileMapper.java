package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserRecommendProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRecommendProfileMapper {
    UserRecommendProfile selectByUserId(@Param("userId") Integer userId);

    int insert(UserRecommendProfile profile);

    int update(UserRecommendProfile profile);

    int deleteByUserId(@Param("userId") Integer userId);
}

