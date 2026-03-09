package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.service.NovelBookMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NovelBookMainServiceImpl implements NovelBookMainService {

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;

    @Autowired
    private NovelVolumeMapper novelVolumeMapper;

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
    public List<Map<String, Object>> getNovelsByDaily() {
        return convertToMapList(novelBookMainMapper.selectNovelsByDaily());
    }

    @Override
    public List<Map<String, Object>> getNovelsByFantasy() {
        return convertToMapList(novelBookMainMapper.selectNovelsByFantasy());
    }

    @Override
    public List<Map<String, Object>> getNovelsBySchool() {
        return convertToMapList(novelBookMainMapper.selectNovelsBySchool());
    }

    @Override
    public List<Map<String, Object>> getNovelsByAdventure() {
        return convertToMapList(novelBookMainMapper.selectNovelsByAdventure());
    }

    @Override
    public List<Map<String, Object>> getNovelsByIsekai() {
        return convertToMapList(novelBookMainMapper.selectNovelsByIsekai());
    }

    @Override
    public List<Map<String, Object>> getNovelsByRelax() {
        return convertToMapList(novelBookMainMapper.selectNovelsByRelax());
    }

    @Override
    public List<Map<String, Object>> getNovelsByFunny() {
        return convertToMapList(novelBookMainMapper.selectNovelsByFunny());
    }

    @Override
    public List<Map<String, Object>> getNovelsByHealing() {
        return convertToMapList(novelBookMainMapper.selectNovelsByHealing());
    }

    @Override
    public List<Map<String, Object>> getNovelsByDepressing() {
        return convertToMapList(novelBookMainMapper.selectNovelsByDepressing());
    }

    @Override
    public List<Map<String, Object>> getNovelsBySweet() {
        return convertToMapList(novelBookMainMapper.selectNovelsBySweet());
    }

    @Override
    public List<Map<String, Object>> getNovelsByHotblood() {
        return convertToMapList(novelBookMainMapper.selectNovelsByHotblood());
    }

    @Override
    public List<Map<String, Object>> getNovelsByLove() {
        return convertToMapList(novelBookMainMapper.selectNovelsByLove());
    }

    @Override
    public List<Map<String, Object>> getNovelsByGrowth() {
        return convertToMapList(novelBookMainMapper.selectNovelsByGrowth());
    }

    @Override
    public List<Map<String, Object>> getNovelsByIntellect() {
        return convertToMapList(novelBookMainMapper.selectNovelsByIntellect());
    }

    @Override
    public List<Map<String, Object>> getNovelsBySuspense() {
        return convertToMapList(novelBookMainMapper.selectNovelsBySuspense());
    }

    @Override
    public List<Map<String, Object>> getNovelsByDeduction() {
        return convertToMapList(novelBookMainMapper.selectNovelsByDeduction());
    }

    @Override
    public List<Map<String, Object>> getNovelsByPsychological() {
        return convertToMapList(novelBookMainMapper.selectNovelsByPsychological());
    }

    @Override
    public List<Map<String, Object>> getNovelsByBattle() {
        return convertToMapList(novelBookMainMapper.selectNovelsByBattle());
    }

    @Override
    public List<Map<String, Object>> getNovelsByCompetition() {
        return convertToMapList(novelBookMainMapper.selectNovelsByCompetition());
    }

    @Override
    public List<Map<String, Object>> getNovelsByConstruction() {
        return convertToMapList(novelBookMainMapper.selectNovelsByConstruction());
    }

    @Override
    public List<Map<String, Object>> getNovelsByPalace() {
        return convertToMapList(novelBookMainMapper.selectNovelsByPalace());
    }

    @Override
    public List<Map<String, Object>> getNovelsByVirtualGame() {
        return convertToMapList(novelBookMainMapper.selectNovelsByVirtualGame());
    }

    @Override
    public List<Map<String, Object>> getNovelsByRealistic() {
        return convertToMapList(novelBookMainMapper.selectNovelsByRealistic());
    }

    @Override
    public List<Map<String, Object>> getNovelsByFarming() {
        return convertToMapList(novelBookMainMapper.selectNovelsByFarming());
    }

    @Override
    public List<Map<String, Object>> getNovelsByReincarnation() {
        return convertToMapList(novelBookMainMapper.selectNovelsByReincarnation());
    }

    @Override
    public List<Map<String, Object>> getNovelsByTransmigration() {
        return convertToMapList(novelBookMainMapper.selectNovelsByTransmigration());
    }

    @Override
    public List<Map<String, Object>> getNovelsByMagic() {
        return convertToMapList(novelBookMainMapper.selectNovelsByMagic());
    }

    @Override
    public List<Map<String, Object>> getNovelsByAllAges() {
        return convertToMapList(novelBookMainMapper.selectNovelsByAllAges());
    }

    @Override
    public List<Map<String, Object>> getNovelsByLightYuri() {
        return convertToMapList(novelBookMainMapper.selectNovelsByLightYuri());
    }

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
