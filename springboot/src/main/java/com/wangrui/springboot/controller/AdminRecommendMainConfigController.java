package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.RecommendMainConfigMapper;
import com.wangrui.springboot.mapper.RecommendScoreDebugMapper;
import com.wangrui.springboot.pojo.RecommendMainConfig;
import com.wangrui.springboot.pojo.RecommendScoreDebug;
import com.wangrui.springboot.service.CacheInvalidationService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 主推荐算法全局超参（表 recommend_main_config）；并合并 recommend_score_debug.cf_cooc_unit_bonus（物品协同计分调试）。
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminRecommendMainConfigController {

    @Autowired
    private RecommendMainConfigMapper recommendMainConfigMapper;
    @Autowired
    private RecommendScoreDebugMapper recommendScoreDebugMapper;
    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    @GetMapping("/recommend-main-config")
    public Result<RecommendMainConfig> get() {
        RecommendMainConfig row;
        try {
            row = recommendMainConfigMapper.selectById(1);
        } catch (Exception e) {
            row = null;
        }
        if (row == null) {
            row = RecommendMainConfig.defaultConfig();
        }
        attachCfCoocDebug(row);
        return Result.success(row);
    }

    @PutMapping("/recommend-main-config")
    public Result<Void> save(@RequestBody RecommendMainConfig body) {
        if (body == null) {
            return Result.error("请求体不能为空");
        }
        String err = validate(body);
        if (err != null) {
            return Result.error(err);
        }
        if (body.getCfCoocUnitBonus() != null) {
            double b = body.getCfCoocUnitBonus().doubleValue();
            if (b < 0 || b > 1.0) {
                return Result.error("cf_cooc_unit_bonus 须在 0～1 之间");
            }
        }
        body.setId(1);
        try {
            if (recommendMainConfigMapper.selectById(1) == null) {
                recommendMainConfigMapper.insert(body);
            } else {
                recommendMainConfigMapper.update(body);
            }
        } catch (Exception e) {
            return Result.error("保存失败：请确认已执行 sql 中 recommend_main_config 建表脚本。详情：" + e.getMessage());
        }
        if (body.getCfCoocUnitBonus() != null) {
            try {
                persistCfCoocBonus(body.getCfCoocUnitBonus());
            } catch (Exception e) {
                return Result.error("保存协同计分调试失败（请确认已执行 novel_db 中 recommend_score_debug 表）：" + e.getMessage());
            }
        }
        cacheInvalidationService.invalidateRecommendMainConfig();
        return Result.success(null);
    }

    private void attachCfCoocDebug(RecommendMainConfig out) {
        try {
            RecommendScoreDebug dbg = recommendScoreDebugMapper.selectById(1);
            if (dbg != null) {
                out.setCfCoocUnitBonus(dbg.getCfCoocUnitBonus() != null ? dbg.getCfCoocUnitBonus() : BigDecimal.ZERO);
                out.setScoreDebugUpdatedAt(dbg.getUpdatedAt());
            } else {
                out.setCfCoocUnitBonus(BigDecimal.ZERO);
            }
        } catch (Exception ignored) {
            out.setCfCoocUnitBonus(BigDecimal.ZERO);
        }
    }

    private void persistCfCoocBonus(BigDecimal cfBonus) {
        RecommendScoreDebug dbg = recommendScoreDebugMapper.selectById(1);
        if (dbg == null) {
            dbg = RecommendScoreDebug.defaultRow();
            dbg.setCfCoocUnitBonus(cfBonus);
            recommendScoreDebugMapper.insert(dbg);
        } else {
            dbg.setCfCoocUnitBonus(cfBonus);
            recommendScoreDebugMapper.update(dbg);
        }
    }

    private static String validate(RecommendMainConfig c) {
        double pop = bd(c.getPopWeight(), -1);
        if (pop < 0 || pop > 1.0) {
            return "pop_weight 须在 0～1 之间";
        }
        double lam = bd(c.getCfBlendLambda(), -1);
        if (lam < 0 || lam > 1.0) {
            return "cf_blend_lambda 须在 0～1 之间";
        }
        int seeds = c.getCfMaxSeeds() != null ? c.getCfMaxSeeds() : -1;
        if (seeds < 1 || seeds > 30) {
            return "cf_max_seeds 须在 1～30 之间";
        }
        int pure = c.getCfPureSlotCap() != null ? c.getCfPureSlotCap() : -1;
        if (pure < 0 || pure > 20) {
            return "cf_pure_slot_cap 须在 0～20 之间";
        }
        int pool = c.getMmrPoolCap() != null ? c.getMmrPoolCap() : -1;
        if (pool < 10 || pool > 200) {
            return "mmr_pool_cap 须在 10～200 之间";
        }
        double mmrL = bd(c.getMmrLambda(), -1);
        if (mmrL < 0 || mmrL > 1.0) {
            return "mmr_lambda 须在 0～1 之间";
        }
        return null;
    }

    private static double bd(BigDecimal v, double def) {
        if (v == null) return def;
        return v.doubleValue();
    }
}
