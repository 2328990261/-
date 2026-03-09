package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.UserMapper;
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
}
