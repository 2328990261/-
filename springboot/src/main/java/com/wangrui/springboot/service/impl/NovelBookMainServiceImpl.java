package com.wangrui.springboot.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.cache.RedisCacheService;
import com.wangrui.springboot.service.NovelBookMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

/**
 * 小说主表服务实现
 * 负责小说列表查询、按标签查询、搜索及阅读量维护，并对部分结果做缓存
 */
@Service
public class NovelBookMainServiceImpl implements NovelBookMainService {

    // 小说主表数据访问对象
    @Autowired
    private NovelBookMainMapper novelBookMainMapper;

    // 小说分卷数据访问对象
    @Autowired
    private NovelVolumeMapper novelVolumeMapper;
    // Redis缓存服务
    @Autowired
    private RedisCacheService redisCacheService;

    /** 查询全部小说，并转换为接口返回的Map结构 */
    @Override
    public List<Map<String, Object>> getAllNovels() {
        // 1. 查询全部小说
        List<NovelBook> novels = novelBookMainMapper.selectAllNovels();
        // 2. 初始化结果集
        List<Map<String, Object>> result = new ArrayList<>();
        // 3. 遍历数据并转换为Map
        for (NovelBook novel : novels) {
            Map<String, Object> novelMap = new HashMap<>();
            novelMap.put("id", novel.getId());
            novelMap.put("bookMainName", novel.getBookMainName());
            novelMap.put("author", novel.getAuthor());
            novelMap.put("label", novel.getLabel());
            novelMap.put("cover", novel.getCover());
            novelMap.put("readCount", novel.getReadCount());
            novelMap.put("createTime", novel.getCreateTime());
            novelMap.put("coverUrl", "/cover/" + novel.getCover());
            result.add(novelMap);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getNovelsByLabels(List<String> labels) {
        // 1. 按多个标签查询小说
        List<NovelBook> novels = novelBookMainMapper.selectNovelsByLabels(labels);
        // 2. 初始化结果集
        List<Map<String, Object>> result = new ArrayList<>();
        // 3. 遍历数据并转换为Map
        for (NovelBook novel : novels) {
            Map<String, Object> novelMap = new HashMap<>();
            novelMap.put("id", novel.getId());
            novelMap.put("bookMainName", novel.getBookMainName());
            novelMap.put("author", novel.getAuthor());
            novelMap.put("label", novel.getLabel());
            novelMap.put("cover", novel.getCover());
            novelMap.put("readCount", novel.getReadCount());
            novelMap.put("coverUrl", "/cover/" + novel.getCover());
            result.add(novelMap);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getNovelsByLabel(String label) {
        // 1. 生成缓存键（中文标签映射为英文代码）
        String cacheKey = "novel:public:label:" + LABEL_CODE_MAP.getOrDefault(label, label);
        // 2. 尝试从缓存读取
        List<Map<String, Object>> cached = redisCacheService.get(
                cacheKey, new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) {
            return cached;
        }
        // 3. 缓存未命中则查询数据库
        List<Map<String, Object>> result = convertToMapList(novelBookMainMapper.selectNovelsByLabel(label));
        // 4. 写入缓存并返回
        redisCacheService.set(cacheKey, result, redisCacheService.properties().getPublicTtl());
        return result;
    }

    private static final Map<String, String> LABEL_CODE_MAP = Map.ofEntries(
            Map.entry("日常", "daily"),
            Map.entry("奇幻", "fantasy"),
            Map.entry("校园", "school"),
            Map.entry("冒险", "adventure"),
            Map.entry("异世界", "isekai"),
            Map.entry("轻松", "relax"),
            Map.entry("搞笑", "funny"),
            Map.entry("治愈", "healing"),
            Map.entry("致郁", "depressing"),
            Map.entry("甜宠", "sweet"),
            Map.entry("热血", "hotblood"),
            Map.entry("恋爱", "love"),
            Map.entry("成长", "growth"),
            Map.entry("智斗", "intellect"),
            Map.entry("悬疑", "suspense"),
            Map.entry("推理", "deduction"),
            Map.entry("心理惊悚", "psychological"),
            Map.entry("战斗", "battle"),
            Map.entry("竞技", "competition"),
            Map.entry("基建", "construction"),
            Map.entry("宫廷", "palace"),
            Map.entry("虚拟网游", "virtual-game"),
            Map.entry("现实题材", "realistic"),
            Map.entry("种田文", "farming"),
            Map.entry("转生", "reincarnation"),
            Map.entry("穿越", "transmigration"),
            Map.entry("魔法", "magic"),
            Map.entry("全年龄", "all-ages"),
            Map.entry("轻百", "light-yuri")
    );

    /** 将小说实体列表转换为接口返回的Map结构 */
    private List<Map<String, Object>> convertToMapList(List<NovelBook> novels) {
        // 1. 初始化结果集
        List<Map<String, Object>> result = new ArrayList<>();
        // 2. 遍历数据并转换为Map
        for (NovelBook novel : novels) {
            Map<String, Object> novelMap = new HashMap<>();
            novelMap.put("id", novel.getId());
            novelMap.put("bookMainName", novel.getBookMainName());
            novelMap.put("author", novel.getAuthor());
            novelMap.put("label", novel.getLabel());
            novelMap.put("cover", novel.getCover());
            novelMap.put("readCount", novel.getReadCount());
            novelMap.put("createTime", novel.getCreateTime());
            novelMap.put("coverUrl", "/cover/" + novel.getCover());
            result.add(novelMap);
        }
        return result;
    }
    
    /** 增加指定小说的阅读量 */
    @Override
    public void increaseReadCount(Integer id) {
        novelBookMainMapper.increaseReadCount(id);
    }

    /** 搜索小说：ID精确查询优先，否则按书名/作者模糊查询；无关键词时返回空列表 */
    @Override
    public List<Map<String, Object>> searchNovels(String keyword) {
        // 1. 空关键词直接返回空列表
        if (keyword == null || (keyword = keyword.trim()).isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 尝试按ID精确查询
        try {
            int id = Integer.parseInt(keyword);
            NovelBook one = novelBookMainMapper.selectNovelById(id);
            if (one != null) {
                return convertToMapList(Collections.singletonList(one));
            }
        } catch (NumberFormatException ignored) {
        }
        // 3. 非ID则按书名/作者模糊查询
        return convertToMapList(novelBookMainMapper.selectNovelsByKeyword(keyword));
    }

}