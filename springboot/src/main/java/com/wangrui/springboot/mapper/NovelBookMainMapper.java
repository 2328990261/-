package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.NovelBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NovelBookMainMapper {
    // 和XML里id="selectAllNovels"完全一致
    List<NovelBook> selectAllNovels();

    // 新增：按多个标签查询小说
    List<NovelBook> selectNovelsByLabels(@Param("labels") List<String> labels);

    // 按标签查询小说
    List<NovelBook> selectNovelsByLabel(@Param("label") String label);

    // 根据ID查单本小说
    NovelBook selectNovelById(@Param("id") Integer id);

    // 新增：按阅读量排序查所有小说（sortType传"ASC"或"DESC"）
    List<NovelBook> selectAllNovelsOrderByReadCount(@Param("sortType") String sortType);

    // 新增：模糊查询（关键词匹配小说名/作者）
    List<NovelBook> selectNovelsByKeyword(@Param("keyword") String keyword);

    // 新增：分页查询小说（offset=起始索引，pageSize=每页条数）
    List<NovelBook> selectNovelsByPage(
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    // 新增：查询小说总数量
    Integer selectNovelTotalCount();

    // 新增：更新阅读量（ID对应的小说阅读量+1）
    int updateReadCount(@Param("id") Integer id);

    int insertNovel(NovelBook novelBook);

    // 后台：分页与总数（status 可选）
    List<NovelBook> selectNovelsByPageAdmin(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize, @Param("status") Integer status);
    Integer selectNovelTotalCountAdmin(@Param("status") Integer status);

    // 后台：分页与总数（支持标签和关键词筛选）
    List<NovelBook> selectNovelsByPageAdminWithFilter(
            @Param("offset") Integer offset, 
            @Param("pageSize") Integer pageSize, 
            @Param("status") Integer status,
            @Param("tag") String tag,
            @Param("keyword") String keyword);
    Integer selectNovelTotalCountAdminWithFilter(
            @Param("status") Integer status,
            @Param("tag") String tag,
            @Param("keyword") String keyword);

    int updateNovel(NovelBook novelBook);
    void updateNovelStatus(@Param("id") Integer id, @Param("status") Integer status);

    void increaseReadCount(@Param("id") Integer id);

    Long selectSumReadCount();
}