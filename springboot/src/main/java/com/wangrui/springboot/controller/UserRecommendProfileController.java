package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.UserRecommendProfileMapper;
import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.mapper.UserDislikeMapper;
import com.wangrui.springboot.mapper.UserTagWeightMapper;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.pojo.UserFinishedNovel;
import com.wangrui.springboot.pojo.UserTagWeight;
import com.wangrui.springboot.pojo.UserTagWeightOverride;
import com.wangrui.springboot.pojo.UserRecommendProfile;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.TagService;
import com.wangrui.springboot.service.UserService;
import com.wangrui.springboot.util.RecommendTagDislikeConstants;
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
    private UserService userService;

    @Autowired
    private NovelBookMainService novelBookMainService;

    @Autowired
    private UserCommentMapper userCommentMapper;

    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;

    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;

    @Autowired
    private UserTagWeightMapper userTagWeightMapper;

    @Autowired
    private UserDislikeMapper userDislikeMapper;

    @Autowired
    private TagService tagService;

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
        List<UserTagWeight> list = computeEffectiveUserTagWeights(userId);
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

    private List<UserTagWeight> computeEffectiveUserTagWeights(Integer userId) {
        // 1) 动态权重（基于行为）
        List<UserTagWeight> dynamic = computeDynamicUserTagWeights(userId);
        Map<String, Double> dynamicMap = new HashMap<>();
        for (UserTagWeight d : dynamic) {
            if (d == null || d.getTagName() == null) continue;
            String k = d.getTagName().trim();
            if (k.isEmpty()) continue;
            dynamicMap.put(k, Math.max(d.getWeight(), 0.0));
        }

        // 2) 覆盖项（用户配置）
        Map<String, Double> override = new HashMap<>();
        try {
            List<UserTagWeightOverride> rows = userTagWeightMapper.selectByUserId(userId);
            if (rows != null) {
                for (UserTagWeightOverride r : rows) {
                    if (r == null || r.getTagName() == null || r.getWeight() == null) continue;
                    String k = r.getTagName().trim();
                    if (k.isEmpty()) continue;
                    double v = r.getWeight().doubleValue();
                    if (v < 0) continue;
                    override.put(k, v);
                }
            }
        } catch (BadSqlGrammarException e) {
            // 表未建：直接用动态权重
            return applyDislikedTagPenalty(userId, dynamic);
        }

        if (override.isEmpty()) {
            return applyDislikedTagPenalty(userId, dynamic);
        }

        // 3) 合成：
        // - 用户配置项尽量保持原值（避免“填50%被二次归一化拉大”）
        // - 未配置标签按动态权重分配“剩余额度”
        double sumOverride = 0.0;
        for (Map.Entry<String, Double> e : override.entrySet()) {
            double v = Math.max(e.getValue(), 0.0);
            override.put(e.getKey(), v);
            sumOverride += v;
        }
        double nonOverrideDynamicSum = 0.0;
        for (Map.Entry<String, Double> e : dynamicMap.entrySet()) {
            if (!override.containsKey(e.getKey())) {
                nonOverrideDynamicSum += Math.max(e.getValue(), 0.0);
            }
        }

        Map<String, Double> effective = new HashMap<>();
        if (sumOverride >= 1.0 || nonOverrideDynamicSum <= 0) {
            // 覆盖项总和超出或已覆盖完所有标签：只在覆盖项内部归一化（防止>100%）
            double norm = sumOverride > 0 ? sumOverride : 1.0;
            for (Map.Entry<String, Double> e : override.entrySet()) {
                effective.put(e.getKey(), e.getValue() / norm);
            }
        } else {
            // 保持覆盖项原值，剩余权重由动态项补齐
            for (Map.Entry<String, Double> e : override.entrySet()) {
                effective.put(e.getKey(), e.getValue());
            }
            double remain = 1.0 - sumOverride;
            for (Map.Entry<String, Double> e : dynamicMap.entrySet()) {
                if (override.containsKey(e.getKey())) continue;
                double dw = Math.max(e.getValue(), 0.0);
                if (dw <= 0) continue;
                effective.put(e.getKey(), remain * (dw / nonOverrideDynamicSum));
            }
        }

        List<UserTagWeight> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : effective.entrySet()) {
            boolean ov = override.containsKey(e.getKey());
            out.add(new UserTagWeight(e.getKey(), e.getValue(), e.getValue(), ov));
        }
        out.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        return applyDislikedTagPenalty(userId, out);
    }

    /**
     * 将 user_dislike_tag 中的标签在占比上乘以 {@link RecommendTagDislikeConstants#DISLIKED_TAG_WEIGHT_FACTOR} 后重新归一化，
     * 使个人中心/配置页展示的权重与主推荐链路一致。
     */
    private List<UserTagWeight> applyDislikedTagPenalty(Integer userId, List<UserTagWeight> list) {
        if (list == null || list.isEmpty() || userId == null) {
            return list;
        }
        Set<String> disliked = new HashSet<>();
        try {
            List<String> tags = userDislikeMapper.selectTagsByUserId(userId);
            if (tags != null) {
                for (String t : tags) {
                    if (t == null) {
                        continue;
                    }
                    String k = t.trim();
                    if (!k.isEmpty()) {
                        disliked.add(k);
                    }
                }
            }
        } catch (BadSqlGrammarException e) {
            return list;
        }
        if (disliked.isEmpty()) {
            return list;
        }

        Map<String, Double> w = new LinkedHashMap<>();
        Map<String, Boolean> overridden = new HashMap<>();
        for (UserTagWeight u : list) {
            if (u == null || u.getTagName() == null) {
                continue;
            }
            String k = u.getTagName().trim();
            if (k.isEmpty()) {
                continue;
            }
            double v = Math.max(u.getWeight(), 0.0);
            if (disliked.contains(k)) {
                v *= RecommendTagDislikeConstants.DISLIKED_TAG_WEIGHT_FACTOR;
            }
            w.put(k, v);
            overridden.put(k, u.isOverridden());
        }
        double sum = 0.0;
        for (double v : w.values()) {
            sum += v;
        }
        if (sum <= 0) {
            return list;
        }
        List<UserTagWeight> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : w.entrySet()) {
            double nw = e.getValue() / sum;
            out.add(new UserTagWeight(e.getKey(), nw, nw, overridden.getOrDefault(e.getKey(), false)));
        }
        out.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        return out;
    }

    private List<UserTagWeight> computeDynamicUserTagWeights(Integer userId) {
        // 权重强弱：评论 > 阅读 > 收藏；并且都是小数，避免“1,1 带偏”
        final double W_COLLECTION = 0.030;
        final double W_READ_PROGRESS = 0.050;
        final double W_READ_DURATION = 0.030;
        final double W_COMMENT = 0.080;
        final int COMMENT_CAP = 3;
        // 关键：给“所有标签”一个统一基线，避免只出现少数标签时其它标签权重归零导致推荐被锁死
        final double BASELINE_PER_TAG = 0.010;

        // 1) 建立 novelId -> label 映射（仅上架小说）
        Map<Integer, String> labelByNovel = new HashMap<>();
        List<Map<String, Object>> all = novelBookMainService.getAllNovels();
        if (all != null) {
            for (Map<String, Object> b : all) {
                Integer nid = parseInt(b.get("id"));
                if (nid == null) continue;
                Object label = b.get("label");
                if (label != null) {
                    labelByNovel.put(nid, label.toString());
                }
            }
        }

        Map<String, Double> raw = new HashMap<>();

        // 0) 基线：所有系统标签等权起步（用户无行为时仍会走 defaultFromTagTable；有少量行为时也不会让其它标签消失）
        List<Tag> sysTags;
        try {
            sysTags = tagService.listAll();
        } catch (Exception e) {
            sysTags = Collections.emptyList();
        }
        if (sysTags != null) {
            for (Tag t : sysTags) {
                if (t == null || t.getName() == null) continue;
                String name = t.getName().trim();
                if (name.isEmpty()) continue;
                raw.put(name, raw.getOrDefault(name, 0.0) + BASELINE_PER_TAG);
            }
        }

        // 2) 收藏：直接用收藏列表里自带的 label
        List<Map<String, Object>> collections = userService.getCollectionList(userId);
        if (collections != null) {
            for (Map<String, Object> row : collections) {
                String label = row.get("label") == null ? null : row.get("label").toString();
                addLabelRaw(raw, label, W_COLLECTION);
            }
        }

        // 3) 阅读历史：进度 + 时长（归一化）
        List<UserReadingHistory> histories = userReadingHistoryMapper.selectByUserId(userId);
        if (histories != null && !histories.isEmpty()) {
            // 仅取最近 10 条（按 last_read_time desc 已排序）
            int limit = Math.min(10, histories.size());
            int maxDur = 1;
            for (int i = 0; i < limit; i++) {
                UserReadingHistory h = histories.get(i);
                if (h == null) continue;
                maxDur = Math.max(maxDur, safeInt(h.getReadDuration(), 0));
            }
            double logMax = Math.log1p(maxDur);
            for (int i = 0; i < limit; i++) {
                UserReadingHistory h = histories.get(i);
                if (h == null || h.getNovelId() == null) continue;
                String label = labelByNovel.get(h.getNovelId());
                double p = Math.min(Math.max(safeInt(h.getReadProgress(), 0), 0), 100) / 100.0;
                double durNorm = Math.log1p(Math.max(safeInt(h.getReadDuration(), 0), 0)) / logMax;
                double add = (W_READ_PROGRESS * p) + (W_READ_DURATION * durNorm);
                addLabelRaw(raw, label, add);
            }
        }

        // 4) 评论：按用户在同一本书的评论次数封顶
        List<com.wangrui.springboot.pojo.UserComment> comments = userCommentMapper.selectByUserId(userId);
        if (comments != null && !comments.isEmpty()) {
            Map<Integer, Integer> cntByNovel = new HashMap<>();
            for (com.wangrui.springboot.pojo.UserComment c : comments) {
                if (c == null || c.getNovelId() == null) continue;
                cntByNovel.put(c.getNovelId(), cntByNovel.getOrDefault(c.getNovelId(), 0) + 1);
            }
            for (Map.Entry<Integer, Integer> e : cntByNovel.entrySet()) {
                int n = Math.min(e.getValue(), COMMENT_CAP);
                String label = labelByNovel.get(e.getKey());
                addLabelRaw(raw, label, W_COMMENT * n);
            }
        }

        // 5) 归一化成占比
        double sum = 0.0;
        for (double v : raw.values()) sum += Math.max(v, 0.0);
        if (sum <= 0) {
            // 新用户/无行为：回退到系统标签表默认权重（tag.recommend_weight）
            return defaultFromTagTable();
        }
        List<UserTagWeight> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : raw.entrySet()) {
            if (e.getKey() == null || e.getKey().trim().isEmpty()) continue;
            double r = Math.max(e.getValue(), 0.0);
            out.add(new UserTagWeight(e.getKey(), r / sum, r));
        }
        out.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        return out;
    }

    private List<UserTagWeight> defaultFromTagTable() {
        List<Tag> tags;
        try {
            tags = tagService.listAll();
        } catch (Exception e) {
            tags = Collections.emptyList();
        }
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        // 新用户默认：所有标签等权，避免先验偏置。
        List<String> names = new ArrayList<>();
        for (Tag t : tags) {
            if (t == null || t.getName() == null) continue;
            String name = t.getName().trim();
            if (name.isEmpty()) continue;
            names.add(name);
        }
        if (names.isEmpty()) {
            return Collections.emptyList();
        }
        double uniform = 1.0 / names.size();
        List<UserTagWeight> out = new ArrayList<>();
        for (String name : names) {
            out.add(new UserTagWeight(name, uniform, uniform));
        }
        out.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        return out;
    }

    private void addLabelRaw(Map<String, Double> raw, String label, double add) {
        if (label == null || label.trim().isEmpty() || add <= 0) return;
        String[] parts = label.split("[,，]");
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (t.isEmpty()) continue;
            raw.put(t, raw.getOrDefault(t, 0.0) + add);
        }
    }

    private Integer parseInt(Object o) {
        if (o == null) return null;
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private int safeInt(Integer v, int def) {
        return v != null ? v : def;
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

