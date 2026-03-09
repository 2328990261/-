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

    // 按标签查询小说（为每个标签单独写方法）
    // 第1行标签
    List<Map<String, Object>> getNovelsByDaily();
    List<Map<String, Object>> getNovelsByFantasy();
    List<Map<String, Object>> getNovelsBySchool();
    List<Map<String, Object>> getNovelsByAdventure();
    List<Map<String, Object>> getNovelsByIsekai();

    // 第2行标签
    List<Map<String, Object>> getNovelsByRelax();
    List<Map<String, Object>> getNovelsByFunny();
    List<Map<String, Object>> getNovelsByHealing();
    List<Map<String, Object>> getNovelsByDepressing();
    List<Map<String, Object>> getNovelsBySweet();
    List<Map<String, Object>> getNovelsByHotblood();
    List<Map<String, Object>> getNovelsByLove();
    List<Map<String, Object>> getNovelsByGrowth();

    // 第3行标签
    List<Map<String, Object>> getNovelsByIntellect();
    List<Map<String, Object>> getNovelsBySuspense();
    List<Map<String, Object>> getNovelsByDeduction();
    List<Map<String, Object>> getNovelsByPsychological();
    List<Map<String, Object>> getNovelsByBattle();
    List<Map<String, Object>> getNovelsByCompetition();
    List<Map<String, Object>> getNovelsByConstruction();

    // 第4行标签
    List<Map<String, Object>> getNovelsByPalace();
    List<Map<String, Object>> getNovelsByVirtualGame();
    List<Map<String, Object>> getNovelsByRealistic();
    List<Map<String, Object>> getNovelsByFarming();
    List<Map<String, Object>> getNovelsByReincarnation();
    List<Map<String, Object>> getNovelsByTransmigration();
    List<Map<String, Object>> getNovelsByMagic();

    // 第5行标签
    List<Map<String, Object>> getNovelsByAllAges();
    List<Map<String, Object>> getNovelsByLightYuri();

    // 增加阅读量
    void increaseReadCount(Integer id);

    /** 搜索：支持 ID 精确查询，或 书名/作者 模糊查询 */
    List<Map<String, Object>> searchNovels(String keyword);
}
