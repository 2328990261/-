// service/NovelBookService.java
package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;

public interface NovelBookService {
    // 获取小说详情（含分卷）
    NovelBook getNovelDetailByMainId(Integer mainBookId);
    // 获取章节内容
    NovelBookVolume getVolumeContentById(Integer volumeId);
}