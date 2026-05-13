package com.wangrui.springboot.service;

import java.util.List;
import java.util.Map;

/**
 * 基于全站用户行为（收藏、阅读、评论、完读）聚合标签热度，供推荐权重与运营仪表盘使用。
 */
public interface SiteTagHeatService {

    /**
     * 与历史 {@link TagService#getRecommendWeights()} 相同形态：标签名 → 推荐用权重（约 0.1～1.0）。
     */
    Map<String, Double> getGlobalRecommendWeights();

    /**
     * 仪表盘：全站原始分最高的 5 个标签及占比说明。
     */
    List<Map<String, Object>> getTop5HotTags();

    /**
     * tag 表中全部标签：原始热度、占全站原始热度占比、映射推荐权重、库内兜底权重（与推荐配置页一致数据源）。
     */
    List<Map<String, Object>> getAllTagHeatRows();

    /** 最近一次聚合时间戳（毫秒） */
    long getLastComputedAtMillis();
}
