package com.wangrui.springboot.cache;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 缓存失效服务
 * 负责在数据变更时，主动删除或清理相关的缓存键，保证缓存与数据库一致
 */
@Service
public class CacheInvalidationService {
    // 轮播图缓存键
    private static final String CAROUSEL_KEY = "novel:public:carousel";
    // 推荐首页配置缓存键
    private static final String MAIN_CONFIG_KEY = "novel:config:recommend-main";
    // 推荐评论配置缓存键
    private static final String COMMENT_CONFIG_KEY = "novel:config:recommend-comment";
    // 全部分类标签缓存键集合
    private static final List<String> CATEGORY_KEYS = List.of(
            "novel:public:label:daily",
            "novel:public:label:fantasy",
            "novel:public:label:school",
            "novel:public:label:adventure",
            "novel:public:label:isekai",
            "novel:public:label:relax",
            "novel:public:label:funny",
            "novel:public:label:healing",
            "novel:public:label:depressing",
            "novel:public:label:sweet",
            "novel:public:label:hotblood",
            "novel:public:label:love",
            "novel:public:label:growth",
            "novel:public:label:intellect",
            "novel:public:label:suspense",
            "novel:public:label:deduction",
            "novel:public:label:psychological",
            "novel:public:label:battle",
            "novel:public:label:competition",
            "novel:public:label:construction",
            "novel:public:label:palace",
            "novel:public:label:virtual-game",
            "novel:public:label:realistic",
            "novel:public:label:farming",
            "novel:public:label:reincarnation",
            "novel:public:label:transmigration",
            "novel:public:label:magic",
            "novel:public:label:all-ages",
            "novel:public:label:light-yuri"
    );

    // 注入Redis缓存服务
    private final RedisCacheService redisCacheService;

    // 构造方法：注入缓存服务
    public CacheInvalidationService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    /** 失效轮播图缓存：删除轮播图缓存键 */
    public void invalidateCarousel() {
        redisCacheService.delete(CAROUSEL_KEY);
    }

    /** 失效某本书的缓存，同时清理全部分类缓存 */
    public void invalidateBook(Integer bookId) {
        // 1. 删除该书的详情缓存
        redisCacheService.delete("novel:public:book:" + bookId);
        // 2. 删除所有分类列表缓存
        invalidateCategories();
    }

    /** 仅失效某本书的详情缓存，不涉及分类列表 */
    public void invalidateBookDetail(Integer bookId) {
        redisCacheService.delete("novel:public:book:" + bookId);
    }

    /** 失效全部分类标签缓存 */
    public void invalidateCategories() {
        redisCacheService.delete(CATEGORY_KEYS.toArray(String[]::new));
    }

    /** 失效推荐首页配置缓存 */
    public void invalidateRecommendMainConfig() {
        redisCacheService.delete(MAIN_CONFIG_KEY);
    }

    /** 失效推荐评论配置缓存 */
    public void invalidateRecommendCommentConfig() {
        redisCacheService.delete(COMMENT_CONFIG_KEY);
    }
}