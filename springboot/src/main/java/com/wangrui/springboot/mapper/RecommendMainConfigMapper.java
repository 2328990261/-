package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.RecommendMainConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 推荐算法主配置数据访问接口 */
@Mapper
public interface RecommendMainConfigMapper {

    RecommendMainConfig selectById(@Param("id") Integer id);

    int insert(RecommendMainConfig config);

    int update(RecommendMainConfig config);
}