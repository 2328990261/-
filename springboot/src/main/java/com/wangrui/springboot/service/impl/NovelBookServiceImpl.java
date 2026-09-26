// service/impl/NovelBookServiceImpl.java
package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.service.NovelBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // 关键：必须加这个注解

import java.util.List;

@Service // 核心！Spring靠这个注解创建Bean
public class NovelBookServiceImpl implements NovelBookService {

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;

    @Autowired
    private NovelVolumeMapper novelVolumeMapper;
    @Autowired
    private RedisCacheService redisCacheService;

    // 实现获取小说详情方法
    @Override
    public NovelBook getNovelDetailByMainId(Integer mainBookId) {
        String cacheKey = "novel:public:book:" + mainBookId;
        NovelBook cached = redisCacheService.get(cacheKey, NovelBook.class);
        if (cached != null) {
            return cached;
        }
        // 1. 查小说主信息
        NovelBook novelBook = novelBookMainMapper.selectNovelById(mainBookId);
        if (novelBook == null) {
            return null;
        }
        // 2. 查小说分卷/章节（含正文内容）
        List<NovelBookVolume> volumeList = novelVolumeMapper.selectVolumeByMainBookId(mainBookId);
        // 3. 剥离正文内容，仅保留章节元信息（id、卷名、时间）用于缓存，正文通过 /chapter/{id} 实时查询
        for (NovelBookVolume volume : volumeList) {
            volume.setContent(null);
        }
        novelBook.setVolumeList(volumeList);
        redisCacheService.set(cacheKey, novelBook, redisCacheService.properties().getPublicTtl());
        return novelBook;
    }

    // 实现获取章节内容方法
    @Override
    public NovelBookVolume getVolumeContentById(Integer volumeId) {
        return novelVolumeMapper.selectVolumeById(volumeId);
    }
}