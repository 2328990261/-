package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.NovelBookIllustration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelIllustrationMapper {
    int insertIllustration(NovelBookIllustration illustration);

    /** 按分卷查插画，按出现顺序排序（阅读页渲染用） */
    List<NovelBookIllustration> selectByVolumeId(@Param("volumeId") Integer volumeId);
}
