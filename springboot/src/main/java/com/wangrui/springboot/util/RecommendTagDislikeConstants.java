package com.wangrui.springboot.util;

/**
 * 「不感兴趣」标签：在用户标签占比中乘以该系数后再与其它标签一起归一化；
 * 与推荐主链路使用的降权幅度一致（避免展示与打分两套逻辑）。
 */
public final class RecommendTagDislikeConstants {

    public static final double DISLIKED_TAG_WEIGHT_FACTOR = 0.2;

    private RecommendTagDislikeConstants() {}
}
