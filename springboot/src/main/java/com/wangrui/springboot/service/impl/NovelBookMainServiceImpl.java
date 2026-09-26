package com.wangrui.springboot.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.NovelBookMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class NovelBookMainServiceImpl implements NovelBookMainService {

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;

    @Autowired
    private NovelVolumeMapper novelVolumeMapper;
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public List<Map<String, Object>> getAllNovels() {
        List<NovelBook> novels = novelBookMainMapper.selectAllNovels();
        List<Map<String, Object>> result = new ArrayList<>();
        
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
        List<NovelBook> novels = novelBookMainMapper.selectNovelsByLabels(labels);
        List<Map<String, Object>> result = new ArrayList<>();
        
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
        String cacheKey = "novel:public:label:" + LABEL_CODE_MAP.getOrDefault(label, label);
        List<Map<String, Object>> cached = redisCacheService.get(
                cacheKey, new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) {
            return cached;
        }
        List<Map<String, Object>> result = convertToMapList(novelBookMainMapper.selectNovelsByLabel(label));
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

    private List<Map<String, Object>> convertToMapList(List<NovelBook> novels) {
        List<Map<String, Object>> result = new ArrayList<>();
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
    public void increaseReadCount(Integer id) {
        novelBookMainMapper.increaseReadCount(id);
    }

    @Override
    public List<Map<String, Object>> searchNovels(String keyword) {
        if (keyword == null || (keyword = keyword.trim()).isEmpty()) {
            return new ArrayList<>();
        }
        try {
            int id = Integer.parseInt(keyword);
            NovelBook one = novelBookMainMapper.selectNovelById(id);
            if (one != null) {
                return convertToMapList(Collections.singletonList(one));
            }
        } catch (NumberFormatException ignored) {
        }
        return convertToMapList(novelBookMainMapper.selectNovelsByKeyword(keyword));
    }

}