package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.NovelBookIllustration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 小说插画数据访问接口，负责插画的写入与按分卷查询 */
@Mapper
public interface NovelIllustrationMapper {
    int insertIllustration(NovelBookIllustration illustration);

    /** 按分卷查插画，按出现顺序排序（阅读页渲染用） */
    List<NovelBookIllustration> selectByVolumeId(@Param("volumeId") Integer volumeId);
}