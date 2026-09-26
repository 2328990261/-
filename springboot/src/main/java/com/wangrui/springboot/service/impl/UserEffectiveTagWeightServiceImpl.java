package com.wangrui.springboot.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.mapper.UserDislikeMapper;
import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.mapper.UserTagWeightMapper;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.pojo.UserComment;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.pojo.UserTagWeight;
import com.wangrui.springboot.pojo.UserTagWeightOverride;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.TagService;
import com.wangrui.springboot.service.UserEffectiveTagWeightService;
import com.wangrui.springboot.service.UserService;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.util.RecommendTagDislikeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserEffectiveTagWeightServiceImpl implements UserEffectiveTagWeightService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserEffectiveTagWeightServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private NovelBookMainService novelBookMainService;

    @Autowired
    private UserCommentMapper userCommentMapper;

    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;

    @Autowired
    private UserTagWeightMapper userTagWeightMapper;

    @Autowired
    private UserDislikeMapper userDislikeMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public Map<String, Double> effectiveWeightsByTagName(Integer userId) {
        List<UserTagWeight> list = computeEffectiveUserTagWeights(userId);
        Map<String, Double> m = new HashMap<>();
        if (list == null) {
            return m;
        }
        for (UserTagWeight u : list) {
            if (u == null || u.getTagName() == null) {
                continue;
            }
            String k = u.getTagName().trim();
            if (k.isEmpty()) {
                continue;
            }
            m.put(k, u.getWeight());
        }
        return m;
    }

    @Override
    public List<UserTagWeight> computeEffectiveUserTagWeights(Integer userId) {
        long version = redisCacheService.getUserRecommendationVersion(userId);
        String cacheKey = version > 0 ? "novel:rec:weights:" + userId + ":" + version : null;
        if (cacheKey != null) {
            List<UserTagWeight> cached = redisCacheService.get(
                    cacheKey, new TypeReference<List<UserTagWeight>>() {});
            if (cached != null) {
                return cached;
            }
        }
        List<UserTagWeight> computed = computeEffectiveUserTagWeightsFromDatabase(userId);
        if (cacheKey != null) {
            redisCacheService.set(cacheKey, computed,
                    redisCacheService.properties().getRecommendTtl());
        }
        return computed;
    }

    private List<UserTagWeight> computeEffectiveUserTagWeightsFromDatabase(Integer userId) {
        List<UserTagWeight> dynamic = computeDynamicUserTagWeights(userId);
        Map<String, Double> dynamicMap = new HashMap<>();
        for (UserTagWeight d : dynamic) {
            if (d == null || d.getTagName() == null) {
                continue;
            }
            String k = d.getTagName().trim();
            if (k.isEmpty()) {
                continue;
            }
            dynamicMap.put(k, Math.max(d.getWeight(), 0.0));
        }

        Map<String, Double> override = new HashMap<>();
        try {
            List<UserTagWeightOverride> rows = userTagWeightMapper.selectByUserId(userId);
            if (rows != null) {
                for (UserTagWeightOverride r : rows) {
                    if (r == null || r.getTagName() == null || r.getWeight() == null) {
                        continue;
                    }
                    String k = r.getTagName().trim();
                    if (k.isEmpty()) {
                        continue;
                    }
                    double v = r.getWeight().doubleValue();
                    if (v < 0) {
                        continue;
                    }
                    override.put(k, v);
                }
            }
        } catch (BadSqlGrammarException e) {
            return applyDislikedTagPenalty(userId, dynamic);
        }

        if (override.isEmpty()) {
            return applyDislikedTagPenalty(userId, dynamic);
        }

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
            double norm = sumOverride > 0 ? sumOverride : 1.0;
            for (Map.Entry<String, Double> e : override.entrySet()) {
                effective.put(e.getKey(), e.getValue() / norm);
            }
        } else {
            for (Map.Entry<String, Double> e : override.entrySet()) {
                effective.put(e.getKey(), e.getValue());
            }
            double remain = 1.0 - sumOverride;
            for (Map.Entry<String, Double> e : dynamicMap.entrySet()) {
                if (override.containsKey(e.getKey())) {
                    continue;
                }
                double dw = Math.max(e.getValue(), 0.0);
                if (dw <= 0) {
                    continue;
                }
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
        final double W_COLLECTION = 0.030;
        final double W_READ_PROGRESS = 0.050;
        final double W_READ_DURATION = 0.030;
        final double W_COMMENT = 0.080;
        final int COMMENT_CAP = 3;
        final double BASELINE_PER_TAG = 0.010;

        Map<Integer, String> labelByNovel = new HashMap<>();
        List<Map<String, Object>> all = novelBookMainService.getAllNovels();
        if (all != null) {
            for (Map<String, Object> b : all) {
                Integer nid = parseInt(b.get("id"));
                if (nid == null) {
                    continue;
                }
                Object label = b.get("label");
                if (label != null) {
                    labelByNovel.put(nid, label.toString());
                }
            }
        }

        Map<String, Double> raw = new HashMap<>();

        List<Tag> sysTags;
        try {
            sysTags = tagService.listAll();
        } catch (Exception e) {
            sysTags = Collections.emptyList();
        }
        if (sysTags != null) {
            for (Tag t : sysTags) {
                if (t == null || t.getName() == null) {
                    continue;
                }
                String name = t.getName().trim();
                if (name.isEmpty()) {
                    continue;
                }
                raw.put(name, raw.getOrDefault(name, 0.0) + BASELINE_PER_TAG);
            }
        }

        List<Map<String, Object>> collections = userService.getCollectionList(userId);
        if (collections != null) {
            for (Map<String, Object> row : collections) {
                String label = row.get("label") == null ? null : row.get("label").toString();
                addLabelRaw(raw, label, W_COLLECTION);
            }
        }

        List<UserReadingHistory> histories = userReadingHistoryMapper.selectByUserId(userId);
        if (histories != null && !histories.isEmpty()) {
            int limit = Math.min(10, histories.size());
            int maxDur = 1;
            for (int i = 0; i < limit; i++) {
                UserReadingHistory h = histories.get(i);
                if (h == null) {
                    continue;
                }
                maxDur = Math.max(maxDur, safeInt(h.getReadDuration(), 0));
            }
            double logMax = Math.log1p(maxDur);
            for (int i = 0; i < limit; i++) {
                UserReadingHistory h = histories.get(i);
                if (h == null || h.getNovelId() == null) {
                    continue;
                }
                String label = labelByNovel.get(h.getNovelId());
                double p = Math.min(Math.max(safeInt(h.getReadProgress(), 0), 0), 100) / 100.0;
                double durNorm = Math.log1p(Math.max(safeInt(h.getReadDuration(), 0), 0)) / logMax;
                double add = (W_READ_PROGRESS * p) + (W_READ_DURATION * durNorm);
                addLabelRaw(raw, label, add);
            }
        }

        List<UserComment> comments = userCommentMapper.selectByUserId(userId);
        if (comments != null && !comments.isEmpty()) {
            Map<Integer, Integer> cntByNovel = new HashMap<>();
            for (UserComment c : comments) {
                if (c == null || c.getNovelId() == null) {
                    continue;
                }
                cntByNovel.put(c.getNovelId(), cntByNovel.getOrDefault(c.getNovelId(), 0) + 1);
            }
            for (Map.Entry<Integer, Integer> e : cntByNovel.entrySet()) {
                int n = Math.min(e.getValue(), COMMENT_CAP);
                String label = labelByNovel.get(e.getKey());
                addLabelRaw(raw, label, W_COMMENT * n);
            }
        }

        double sum = 0.0;
        for (double v : raw.values()) {
            sum += Math.max(v, 0.0);
        }
        if (sum <= 0) {
            return defaultFromTagTable();
        }
        List<UserTagWeight> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : raw.entrySet()) {
            if (e.getKey() == null || e.getKey().trim().isEmpty()) {
                continue;
            }
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
        List<String> names = new ArrayList<>();
        for (Tag t : tags) {
            if (t == null || t.getName() == null) {
                continue;
            }
            String name = t.getName().trim();
            if (name.isEmpty()) {
                continue;
            }
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
        if (label == null || label.trim().isEmpty() || add <= 0) {
            return;
        }
        String[] parts = label.split("[,，]");
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (t.isEmpty()) {
                continue;
            }
            raw.put(t, raw.getOrDefault(t, 0.0) + add);
        }
    }

    private Integer parseInt(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private int safeInt(Integer v, int def) {
        return v != null ? v : def;
    }
}
