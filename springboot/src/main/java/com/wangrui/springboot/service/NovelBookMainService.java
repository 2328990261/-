package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import java.util.List;
import java.util.Map;

public interface NovelBookMainService {
    // 对应Mapper的selectAllNovels() → 返回加工后的数据
    List<Map<String, Object>> getAllNovels();

    // 新增：按多个标签查询小说
    List<Map<String, Object>> getNovelsByLabels(List<String> labels);

    // 按标签查询小说（参数化，支持所有标签）
    List<Map<String, Object>> getNovelsByLabel(String label);

    // 增加阅读量
    void increaseReadCount(Integer id);

    /** 搜索：支持 ID 精确查询，或 书名/作者 模糊查询 */
    List<Map<String, Object>> searchNovels(String keyword);
}