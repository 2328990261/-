package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.vo.RecommendBooksResponse;

public interface RecommendMixService {

    /**
     * 混合推荐：标签命中 + 行为加分 + Item-CF + 热度 + MMR；返回书单与 debug。
     */
    RecommendBooksResponse recommend(Integer userId, String sortType);
}
