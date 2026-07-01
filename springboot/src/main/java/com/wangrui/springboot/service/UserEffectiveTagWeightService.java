package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.UserTagWeight;

import java.util.List;
import java.util.Map;

/**
 * 用户有效标签权重（与 GET /api/user/recommend/tag-weights 同源），供个人中心展示与主推荐打分共用。
 */
public interface UserEffectiveTagWeightService {

    List<UserTagWeight> computeEffectiveUserTagWeights(Integer userId);

    /** 标签名 → 归一化权重（0～1，与展示接口一致） */
    Map<String, Double> effectiveWeightsByTagName(Integer userId);
}
