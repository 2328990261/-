package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.RecommendMainConfigMapper;
import com.wangrui.springboot.pojo.RecommendMainConfig;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 主推荐算法全局超参（表 recommend_main_config，作用于 /api/recommend/books 主路径）。
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminRecommendMainConfigController {

    @Autowired
    private RecommendMainConfigMapper recommendMainConfigMapper;

    @GetMapping("/recommend-main-config")
    public Result<RecommendMainConfig> get() {
        RecommendMainConfig row;
        try {
            row = recommendMainConfigMapper.selectById(1);
        } catch (Exception e) {
            row = null;
        }
        return Result.success(row != null ? row : RecommendMainConfig.defaultConfig());
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
        body.setId(1);
        try {
            if (recommendMainConfigMapper.selectById(1) == null) {
                recommendMainConfigMapper.insert(body);
            } else {
                recommendMainConfigMapper.update(body);
            }
        } catch (Exception e) {
            return Result.error("保存失败：请确认已执行 sql/recommend_main_config.sql 建表脚本。详情：" + e.getMessage());
        }
        return Result.success(null);
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
