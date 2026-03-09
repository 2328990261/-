package com.wangrui.springboot.controller;

import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.TagService;
import com.wangrui.springboot.service.UserPreferenceService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommend")
@CrossOrigin
public class RecommendController {
    
    @Autowired
    private UserPreferenceService userPreferenceService;
    
    @Autowired
    private NovelBookMainService novelBookMainService;

    @Autowired
    private TagService tagService;

    private static final double DEFAULT_TAG_WEIGHT = 0.1;
    
    /**
     * 基于标签的协同过滤推荐
     * @param userId 用户ID
     * @param sortType 排序类型：collection(收藏排序) 或 custom(自定义排序)
     * @return 推荐书籍列表
     */
    @GetMapping("/books")
    public Result recommendBooks(@RequestParam("userId") Integer userId, 
                                  @RequestParam("sortType") String sortType) {
        try {
            // 1. 收藏排序：根据收藏与阅读实时计算，不读库；自定义排序：从库读取
            List<UserPreferenceTag> userTags;
            if ("collection".equals(sortType)) {
                userTags = userPreferenceService.getCollectionBasedTags(userId);
            } else {
                userTags = userPreferenceService.getUserPreferenceTagsByType(userId, sortType);
            }
            if (userTags == null || userTags.isEmpty()) {
                return Result.error("请先设置" + ("collection".equals(sortType) ? "收藏一些书籍或设置自定义排序" : "自定义排序") + "标签");
            }
            
            // 打印调试信息
            System.out.println("=== 推荐调试信息 ===");
            System.out.println("用户ID: " + userId);
            System.out.println("排序类型: " + sortType);
            System.out.println("用户标签数量: " + userTags.size());
            for (int i = 0; i < userTags.size(); i++) {
                UserPreferenceTag tag = userTags.get(i);
                System.out.println("标签" + (i+1) + ": " + tag.getTagName() + " (order=" + tag.getTagOrder() + ")");
            }
            
            List<UserPreferenceTag> sortedTags = userTags;
            Map<String, Double> configWeights = tagService.getRecommendWeights();
            Map<String, Double> tagWeights = new HashMap<>();
            for (int i = 0; i < sortedTags.size() && i < 5; i++) {
                String tagName = sortedTags.get(i).getTagName();
                if (tagName == null) continue;
                String trimmed = tagName.trim();
                double weight = configWeights.containsKey(trimmed) ? configWeights.get(trimmed) : DEFAULT_TAG_WEIGHT;
                tagWeights.put(trimmed, weight);
                System.out.println("标签权重: " + trimmed + " = " + weight);
            }
            
            // 4. 获取所有小说
            List<Map<String, Object>> allBooksMap = novelBookMainService.getAllNovels();
            System.out.println("总小说数量: " + allBooksMap.size());
            
            // 5. 计算每本书的匹配度
            List<BookScore> bookScores = new ArrayList<>();
            for (Map<String, Object> bookMap : allBooksMap) {
                double score = calculateBookScore(bookMap, tagWeights);
                if (score > 0) {
                    // 获取阅读量
                    int readCount = 0;
                    Object readCountObj = bookMap.get("readCount");
                    if (readCountObj != null) {
                        readCount = Integer.parseInt(readCountObj.toString());
                    }
                    bookScores.add(new BookScore(bookMap, score, readCount));
                    System.out.println("书籍: " + bookMap.get("bookMainName") + ", 标签: " + bookMap.get("label") + ", 得分: " + score + ", 阅读量: " + readCount);
                }
            }
            
            System.out.println("匹配的书籍数量: " + bookScores.size());
            
            // 6. 按匹配度和阅读量排序：先按匹配度降序，匹配度相同时按阅读量降序
            bookScores.sort((a, b) -> {
                int scoreCompare = Double.compare(b.score, a.score);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return Integer.compare(b.readCount, a.readCount);
            });
            
            // 7. 返回推荐结果
            List<Map<String, Object>> recommendedBooks = bookScores.stream()
                .map(bs -> bs.book)
                .limit(20)  // 返回前20本
                .collect(Collectors.toList());
            
            System.out.println("推荐书籍数量: " + recommendedBooks.size());
            
            return Result.success(recommendedBooks);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("推荐失败：" + e.getMessage());
        }
    }
    
    /**
     * 计算书籍匹配度
     */
    private double calculateBookScore(Map<String, Object> bookMap, Map<String, Double> tagWeights) {
        Object labelObj = bookMap.get("label");
        if (labelObj == null) {
            return 0.0;
        }
        
        String label = labelObj.toString();
        if (label.isEmpty()) {
            return 0.0;
        }
        
        // 分割书籍标签
        String[] bookTags = label.split("[,，]");
        double totalScore = 0.0;
        
        // 计算匹配的标签权重总和
        for (String bookTag : bookTags) {
            String trimmedTag = bookTag.trim();
            if (tagWeights.containsKey(trimmedTag)) {
                totalScore += tagWeights.get(trimmedTag);
            }
        }
        
        return totalScore;
    }
    
    /**
     * 书籍评分内部类
     */
    private static class BookScore {
        Map<String, Object> book;
        double score;
        int readCount;
        
        BookScore(Map<String, Object> book, double score, int readCount) {
            this.book = book;
            this.score = score;
            this.readCount = readCount;
        }
    }
}
