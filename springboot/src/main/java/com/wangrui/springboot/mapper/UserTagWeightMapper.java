package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserTagWeightOverride;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTagWeightMapper {
    List<UserTagWeightOverride> selectByUserId(@Param("userId") Integer userId);

    int upsert(UserTagWeightOverride row);

    int deleteByUserId(@Param("userId") Integer userId);
}

