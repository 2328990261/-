package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.RecommendCommentConfigMapper;
import com.wangrui.springboot.mapper.RecommendScoreDebugMapper;
import com.wangrui.springboot.pojo.RecommendCommentConfig;
import com.wangrui.springboot.pojo.RecommendScoreDebug;
import com.wangrui.springboot.service.CacheInvalidationService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 推荐行为权重（表 recommend_comment_config）：评论 + 阅读历史；并合并 recommend_score_debug.read_record_unit_bonus。
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminRecommendCommentConfigController {

    @Autowired
    private RecommendCommentConfigMapper recommendCommentConfigMapper;
    @Autowired
    private RecommendScoreDebugMapper recommendScoreDebugMapper;
    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    private static RecommendCommentConfig defaultConfig() {
        RecommendCommentConfig c = new RecommendCommentConfig();
        c.setId(1);
        c.setEnabled(1);
        c.setCommentWeight(new BigDecimal("0.020000"));
        c.setCommentCountCap(50);
        c.setReadHistoryEnabled(1);
        c.setReadHistoryWeight(new BigDecimal("0.030000"));
        c.setReadHistoryRecentLimit(5);
        c.setReadRecordUnitBonus(BigDecimal.ZERO);
        return c;
    }

    @GetMapping("/recommend-comment-config")
    public Result<RecommendCommentConfig> get() {
        RecommendCommentConfig row = recommendCommentConfigMapper.selectById(1);
        if (row == null) {
            row = defaultConfig();
        }
        attachReadDebug(row);
        return Result.success(row);
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
        if (body.getReadRecordUnitBonus() != null) {
            double rb = body.getReadRecordUnitBonus().doubleValue();
            if (rb < 0 || rb > 1.0) {
                return Result.error("read_record_unit_bonus 须在 0～1 之间");
            }
        }
        body.setId(1);
        try {
            if (recommendCommentConfigMapper.selectById(1) == null) {
                recommendCommentConfigMapper.insert(body);
            } else {
                recommendCommentConfigMapper.update(body);
            }
        } catch (Exception e) {
            return Result.error("保存行为配置失败：" + e.getMessage());
        }
        if (body.getReadRecordUnitBonus() != null) {
            try {
                persistReadRecordBonus(body.getReadRecordUnitBonus());
            } catch (Exception e) {
                return Result.error("保存阅读计分调试失败（请确认已执行 novel_db 中 recommend_score_debug 表）：" + e.getMessage());
            }
        }
        cacheInvalidationService.invalidateRecommendCommentConfig();
        return Result.success(null);
    }

    private void attachReadDebug(RecommendCommentConfig out) {
        try {
            RecommendScoreDebug dbg = recommendScoreDebugMapper.selectById(1);
            if (dbg != null) {
                out.setReadRecordUnitBonus(dbg.getReadRecordUnitBonus() != null ? dbg.getReadRecordUnitBonus() : BigDecimal.ZERO);
                out.setScoreDebugUpdatedAt(dbg.getUpdatedAt());
            } else {
                out.setReadRecordUnitBonus(BigDecimal.ZERO);
            }
        } catch (Exception ignored) {
            out.setReadRecordUnitBonus(BigDecimal.ZERO);
        }
    }

    private void persistReadRecordBonus(BigDecimal readBonus) {
        RecommendScoreDebug dbg = recommendScoreDebugMapper.selectById(1);
        if (dbg == null) {
            dbg = RecommendScoreDebug.defaultRow();
            dbg.setReadRecordUnitBonus(readBonus);
            recommendScoreDebugMapper.insert(dbg);
        } else {
            dbg.setReadRecordUnitBonus(readBonus);
            recommendScoreDebugMapper.update(dbg);
        }
    }
}
