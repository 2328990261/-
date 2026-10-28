package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.vo.RecommendBooksResponse;

/** 混合推荐服务接口：综合标签、行为、协同过滤、热度与 MMR 计算个性化书单 */
public interface RecommendMixService {

    /**
     * 混合推荐：标签命中 + 行为加分 + Item-CF + 热度 + MMR；返回书单与 debug。
     */
    RecommendBooksResponse recommend(Integer userId, String sortType);
}