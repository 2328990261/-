package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.NovelBookVolume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelVolumeMapper {
    // 根据小说主表id查分卷
    List<NovelBookVolume> selectVolumeByMainBookId(Integer mainBookId);

    /** 后台管理：仅 id、卷名、时间，不含正文 */
    List<NovelBookVolume> selectVolumeMetaByMainBookId(@Param("mainBookId") Integer mainBookId);

    /** 删除分卷，且必须属于指定主书 */
    int deleteVolumeByIdAndMainBook(@Param("volumeId") Integer volumeId, @Param("mainBookId") Integer mainBookId);
    // 根据分卷ID查单条分卷（含content内容，阅读页用）
    NovelBookVolume selectVolumeById(Integer volumeId);
    // 插入新分卷（书籍上架时添加章节内容）
    int insertVolume(NovelBookVolume volume);
}
