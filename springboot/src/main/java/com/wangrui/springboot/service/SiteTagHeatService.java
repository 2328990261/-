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
     * 推荐配置页：tag 表内全部标签的热度、映射权重与占全站热度占比（与聚合缓存同源）。
     */
    List<Map<String, Object>> getAllTagHeatRows();

    /** 最近一次聚合时间戳（毫秒） */
    long getLastComputedAtMillis();

    /**
     * 时序趋势（方案 A）：基于收藏/评论/完读三类带时间戳行为，按日/周/月分桶统计。
     *
     * @param granularity 时间粒度：day（近7天）、week（近8周）、month（近12个月）
     * @param metric      指标：heat（标签走向）、collection（收藏数）、comment（评论数）、finished（完读数）
     * @return 含 buckets（横轴标签）与 series（每条线一个标签或单项指标）的结构
     */
    Map<String, Object> getTagTrend(String granularity, String metric);
}
