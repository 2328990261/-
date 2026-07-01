package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.UserRecommendProfileMapper;
import com.wangrui.springboot.mapper.UserTagWeightMapper;
import com.wangrui.springboot.pojo.UserTagWeight;
import com.wangrui.springboot.pojo.UserTagWeightOverride;
import com.wangrui.springboot.pojo.UserRecommendProfile;
import com.wangrui.springboot.service.UserEffectiveTagWeightService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/user/recommend")
@CrossOrigin
public class UserRecommendProfileController {

    @Autowired
    private UserRecommendProfileMapper userRecommendProfileMapper;

    @Autowired
    private UserTagWeightMapper userTagWeightMapper;

    @Autowired
    private UserEffectiveTagWeightService userEffectiveTagWeightService;

    @GetMapping("/profile")
    public Result<UserRecommendProfile> getProfile(@RequestParam("userId") Integer userId) {
        if (userId == null) {
            return Result.error("userId 不能为空");
        }
        UserRecommendProfile row;
        try {
            row = userRecommendProfileMapper.selectByUserId(userId);
        } catch (BadSqlGrammarException e) {
            // 多数情况是新表尚未执行建表脚本，为避免前端直接崩溃，这里返回默认配置。
            return Result.success(UserRecommendProfile.defaultForUser(userId));
        }
        if (row == null) {
            return Result.success(UserRecommendProfile.defaultForUser(userId));
        }
        return Result.success(row);
    }

    /**
     * 返回“用户标签权重分配”（由 收藏+阅读历史+评论 动态计算并归一化；
     * 若用户在「不感兴趣」中对标签降权，则该标签占比会乘以统一系数后再归一化，与推荐打分一致）。
     * GET /api/user/recommend/tag-weights?userId=...
     */
    @GetMapping("/tag-weights")
    public Result<List<UserTagWeight>> getUserTagWeights(@RequestParam("userId") Integer userId) {
        if (userId == null) {
            return Result.error("userId 不能为空");
        }
        List<UserTagWeight> list = userEffectiveTagWeightService.computeEffectiveUserTagWeights(userId);
        return Result.success(list);
    }

    /**
     * 保存用户标签权重覆盖项（可批量）。
     * PUT /api/user/recommend/tag-weights?userId=...
     */
    @PutMapping("/tag-weights")
    public Result<Void> saveUserTagWeights(@RequestParam("userId") Integer userId, @RequestBody List<UserTagWeightOverride> rows) {
        if (userId == null) return Result.error("userId 不能为空");
        if (rows == null) return Result.error("请求体不能为空");
        try {
            // 简化：先清空再写入，保证“每用户一套”与页面一致
            userTagWeightMapper.deleteByUserId(userId);
            for (UserTagWeightOverride r : rows) {
                if (r == null) continue;
                if (r.getTagName() == null || r.getTagName().trim().isEmpty()) continue;
                if (r.getWeight() == null) continue;
                if (r.getWeight().doubleValue() < 0) continue;
                UserTagWeightOverride x = new UserTagWeightOverride();
                x.setUserId(userId);
                x.setTagName(r.getTagName().trim());
                x.setWeight(r.getWeight());
                userTagWeightMapper.upsert(x);
            }
            return Result.success(null);
        } catch (BadSqlGrammarException e) {
            return Result.error("用户标签权重表不存在，请先执行 sql/user_tag_weight.sql 建表脚本");
        }
    }

    @PutMapping("/profile")
    public Result<Void> saveProfile(@RequestParam("userId") Integer userId, @RequestBody UserRecommendProfile body) {
        if (userId == null) {
            return Result.error("userId 不能为空");
        }
        if (body == null) {
            return Result.error("请求体不能为空");
        }

        body.setUserId(userId);
        normalizeAndValidate(body);

        try {
            if (userRecommendProfileMapper.selectByUserId(userId) == null) {
                userRecommendProfileMapper.insert(body);
            } else {
                userRecommendProfileMapper.update(body);
            }
        } catch (BadSqlGrammarException e) {
            return Result.error("推荐配置表不存在，请先执行 sql/user_recommend_profile.sql 建表脚本");
        }
        return Result.success(null);
    }

    private void normalizeAndValidate(UserRecommendProfile p) {
        p.setCfEnabled(p.getCfEnabled() != null && p.getCfEnabled() == 0 ? 0 : 1);
        p.setExplainEnabled(p.getExplainEnabled() != null && p.getExplainEnabled() == 1 ? 1 : 0);
        p.setMmrEnabled(p.getMmrEnabled() != null && p.getMmrEnabled() == 0 ? 0 : 1);

        p.setwCollection(nonNeg(p.getwCollection(), new BigDecimal("1.000000")));
        p.setwFinished(nonNeg(p.getwFinished(), new BigDecimal("1.000000")));
        p.setwReadProgress(nonNeg(p.getwReadProgress(), new BigDecimal("0.600000")));
        p.setwReadDuration(nonNeg(p.getwReadDuration(), new BigDecimal("0.400000")));
        p.setwComment(nonNeg(p.getwComment(), new BigDecimal("0.700000")));

        p.setColdStartCollectionThreshold(clampInt(p.getColdStartCollectionThreshold(), 0, 50, 3));
        p.setRecentReadLimit(clampInt(p.getRecentReadLimit(), 1, 200, 30));
        p.setTopkSimilarPerSeed(clampInt(p.getTopkSimilarPerSeed(), 1, 200, 50));
        p.setCandidateLimit(clampInt(p.getCandidateLimit(), 10, 2000, 200));
        p.setRecommendLimit(clampInt(p.getRecommendLimit(), 1, 100, 20));
    }

    private BigDecimal nonNeg(BigDecimal v, BigDecimal def) {
        if (v == null) return def;
        if (v.doubleValue() < 0) return def;
        return v;
    }

    private int clampInt(Integer v, int min, int max, int def) {
        if (v == null) return def;
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}

