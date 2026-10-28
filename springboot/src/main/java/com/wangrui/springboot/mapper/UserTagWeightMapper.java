package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.UserTagWeightOverride;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 用户标签权重覆盖数据访问接口，负责读取与维护用户手动设置的标签权重 */
@Mapper
public interface UserTagWeightMapper {
    List<UserTagWeightOverride> selectByUserId(@Param("userId") Integer userId);

    int upsert(UserTagWeightOverride row);

    int deleteByUserId(@Param("userId") Integer userId);
}