package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.RecommendScoreDebug;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 推荐得分调试数据访问接口 */
@Mapper
public interface RecommendScoreDebugMapper {

    RecommendScoreDebug selectById(@Param("id") Integer id);

    int insert(RecommendScoreDebug row);

    int update(RecommendScoreDebug row);
}