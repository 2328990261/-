package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.RecommendCommentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 推荐行为（评论/阅读历史）权重配置数据访问接口 */
@Mapper
public interface RecommendCommentConfigMapper {

    RecommendCommentConfig selectById(@Param("id") Integer id);

    int update(RecommendCommentConfig config);

    int insert(RecommendCommentConfig config);
}