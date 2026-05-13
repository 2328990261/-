package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.RecommendCommentConfigMapper;
import com.wangrui.springboot.pojo.RecommendCommentConfig;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 推荐行为权重（表 recommend_comment_config）：评论 + 阅读历史。收藏不参与。
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminRecommendCommentConfigController {

    @Autowired
    private RecommendCommentConfigMapper recommendCommentConfigMapper;

    private static RecommendCommentConfig defaultConfig() {
        RecommendCommentConfig c = new RecommendCommentConfig();
        c.setId(1);
        c.setEnabled(1);
        c.setCommentWeight(new BigDecimal("0.020000"));
        c.setCommentCountCap(50);
        c.setReadHistoryEnabled(1);
        c.setReadHistoryWeight(new BigDecimal("0.030000"));
        c.setReadHistoryRecentLimit(5);
        return c;
    }

    @GetMapping("/recommend-comment-config")
    public Result<RecommendCommentConfig> get() {
        RecommendCommentConfig row = recommendCommentConfigMapper.selectById(1);
        return Result.success(row != null ? row : defaultConfig());
    }

    @PutMapping("/recommend-comment-config")
    public Result<Void> save(@RequestBody RecommendCommentConfig body) {
        if (body.getEnabled() == null || (body.getEnabled() != 0 && body.getEnabled() != 1)) {
            return Result.error("enabled 须为 0 或 1");
        }
        if (body.getCommentWeight() == null || body.getCommentWeight().doubleValue() < 0) {
            return Result.error("comment_weight 须 >= 0");
        }
        if (body.getCommentCountCap() == null || body.getCommentCountCap() < 1) {
            return Result.error("comment_count_cap 须 >= 1");
        }
        if (body.getReadHistoryEnabled() == null || (body.getReadHistoryEnabled() != 0 && body.getReadHistoryEnabled() != 1)) {
            return Result.error("read_history_enabled 须为 0 或 1");
        }
        if (body.getReadHistoryWeight() == null || body.getReadHistoryWeight().doubleValue() < 0) {
            return Result.error("read_history_weight 须 >= 0");
        }
        if (body.getReadHistoryRecentLimit() == null || body.getReadHistoryRecentLimit() < 1 || body.getReadHistoryRecentLimit() > 50) {
            return Result.error("read_history_recent_limit 须在 1～50 之间");
        }
        body.setId(1);
        if (recommendCommentConfigMapper.selectById(1) == null) {
            recommendCommentConfigMapper.insert(body);
        } else {
            recommendCommentConfigMapper.update(body);
        }
        return Result.success(null);
    }
}
