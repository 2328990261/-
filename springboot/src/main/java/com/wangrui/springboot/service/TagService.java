package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.Tag;

import java.util.List;
import java.util.Map;

public interface TagService {
    List<Tag> listAll();
    Tag getById(Integer id);
    int add(Tag tag);
    int update(Tag tag);
    int delete(Integer id);
    /** 推荐用：标签名 -> 权重，未在表中的标签给默认权重 */
    Map<String, Double> getRecommendWeights();
}
