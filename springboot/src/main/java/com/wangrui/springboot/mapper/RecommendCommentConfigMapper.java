package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.RecommendCommentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendCommentConfigMapper {

    RecommendCommentConfig selectById(@Param("id") Integer id);

    int update(RecommendCommentConfig config);

    int insert(RecommendCommentConfig config);
}
