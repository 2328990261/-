package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.service.SiteTagHeatService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminStatsController {
    @Autowired
    private NovelBookMainMapper novelBookMainMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SiteTagHeatService siteTagHeatService;

    /** 概览：书籍总数、用户总数、总阅读量、总收藏数 */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overview() {
        int totalBooks = novelBookMainMapper.selectNovelTotalCountAdmin(null);
        int totalUsers = userMapper.selectUserCount(null, null);
        Long totalViews = novelBookMainMapper.selectSumReadCount();
        int totalCollections = userMapper.selectCollectionCount();
        Map<String, Object> data = new HashMap<>();
        data.put("totalBooks", totalBooks);
        data.put("totalUsers", totalUsers);
        data.put("totalViews", totalViews != null ? totalViews : 0);
        data.put("totalCollections", totalCollections);
        return Result.success(data);
    }

    /**
     * 全站标签阅读走向：原始热度 Top5 + 当前映射到推荐侧的权重（仅含 tag 表内标签的权重字段；Top5 可含书库 label 中出现的任意字符串）。
     */
    @GetMapping("/stats/tag-site-heat")
    public Result<Map<String, Object>> tagSiteHeat() {
        Map<String, Object> data = new HashMap<>();
        data.put("lastComputedAt", siteTagHeatService.getLastComputedAtMillis());
        data.put("cacheTtlSeconds", 120);
        data.put("top5", siteTagHeatService.getTop5HotTags());
        data.put("description", "来源：全站收藏、阅读历史、评论条数（按书封顶）、完读；标签上按热度映射为 0.1~1.0 推荐权重");
        return Result.success(data);
    }

    /** 推荐配置 / 运营：tag 表全部标签的热度、占比与映射权重（与聚合服务同源缓存）。 */
    @GetMapping("/stats/tag-site-heat-rows")
    public Result<Map<String, Object>> tagSiteHeatRows() {
        Map<String, Object> data = new HashMap<>();
        data.put("lastComputedAt", siteTagHeatService.getLastComputedAtMillis());
        data.put("cacheTtlSeconds", 120);
        data.put("rows", siteTagHeatService.getAllTagHeatRows());
        data.put("description", "来源：全站收藏、阅读历史、评论条数（按书封顶）、完读；标签上按热度映射为 0.1~1.0 推荐权重");
        return Result.success(data);
    }
}
