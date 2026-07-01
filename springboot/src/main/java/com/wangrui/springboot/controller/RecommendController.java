package com.wangrui.springboot.controller;

import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.pojo.vo.RecommendBooksResponse;
import com.wangrui.springboot.service.RecommendMixService;
import com.wangrui.springboot.service.UserPreferenceService;
import com.wangrui.springboot.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/recommend")
@CrossOrigin
public class RecommendController {

    private static final Logger log = LoggerFactory.getLogger(RecommendController.class);

    @Autowired
    private UserPreferenceService userPreferenceService;

    @Autowired
    private RecommendMixService recommendMixService;

    /**
     * 混合推荐：标签命中 + 行为加分 + Item-CF + 热度 + MMR；返回 {@link RecommendBooksResponse}（books + debug）。
     */
    @GetMapping("/books")
    public Result<RecommendBooksResponse> recommendBooks(@RequestParam("userId") Integer userId,
                                                           @RequestParam("sortType") String sortType) {
        try {
            List<UserPreferenceTag> userTags;
            if ("collection".equals(sortType)) {
                userTags = userPreferenceService.getCollectionBasedTags(userId);
            } else {
                userTags = userPreferenceService.getUserPreferenceTagsByType(userId, sortType);
            }
            if (userTags == null || userTags.isEmpty()) {
                return Result.error("请先设置" + ("collection".equals(sortType) ? "收藏一些书籍或设置自定义排序" : "自定义排序") + "标签");
            }

            RecommendBooksResponse body = recommendMixService.recommend(userId, sortType);
            return Result.success(body);
        } catch (Exception e) {
            log.error("推荐失败 userId={} sortType={}", userId, sortType, e);
            return Result.error("推荐失败：" + e.getMessage());
        }
    }
}
