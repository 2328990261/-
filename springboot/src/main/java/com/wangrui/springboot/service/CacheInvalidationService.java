package com.wangrui.springboot.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CacheInvalidationService {
    private static final String CAROUSEL_KEY = "novel:public:carousel";
    private static final String MAIN_CONFIG_KEY = "novel:config:recommend-main";
    private static final String COMMENT_CONFIG_KEY = "novel:config:recommend-comment";
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

    private final RedisCacheService redisCacheService;

    public CacheInvalidationService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public void invalidateCarousel() {
        redisCacheService.delete(CAROUSEL_KEY);
    }

    public void invalidateBook(Integer bookId) {
        redisCacheService.delete("novel:public:book:" + bookId);
        invalidateCategories();
    }

    public void invalidateBookDetail(Integer bookId) {
        redisCacheService.delete("novel:public:book:" + bookId);
    }

    public void invalidateCategories() {
        redisCacheService.delete(CATEGORY_KEYS.toArray(String[]::new));
    }

    public void invalidateRecommendMainConfig() {
        redisCacheService.delete(MAIN_CONFIG_KEY);
    }

    public void invalidateRecommendCommentConfig() {
        redisCacheService.delete(COMMENT_CONFIG_KEY);
    }
}
