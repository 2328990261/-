package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.TagMapper;
import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.SiteTagHeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SiteTagHeatServiceImpl implements SiteTagHeatService {

    private static final long CACHE_TTL_MS = 120_000;

    private static final double W_COLLECTION = 1.0;
    private static final double W_READ_PROGRESS = 0.55;
    private static final double W_READ_DURATION = 0.45;
    private static final double W_COMMENT_PER_NOVEL_UNIT = 0.35;
    private static final int COMMENT_CAP = 20;
    private static final double W_FINISHED = 0.85;

    @Autowired
    private NovelBookMainService novelBookMainService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;
    @Autowired
    private UserCommentMapper userCommentMapper;
    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;
    @Autowired
    private TagMapper tagMapper;

    private final Object lock = new Object();
    private volatile long lastComputedAt;
    private volatile Map<String, Double> cachedWeights = Collections.emptyMap();
    private volatile List<Map<String, Object>> cachedTop5 = Collections.emptyList();
    private volatile List<Map<String, Object>> cachedAllRows = Collections.emptyList();

    @Override
    public Map<String, Double> getGlobalRecommendWeights() {
        ensureFresh();
        return cachedWeights;
    }

    @Override
    public List<Map<String, Object>> getTop5HotTags() {
        ensureFresh();
        return cachedTop5;
    }

    @Override
    public List<Map<String, Object>> getAllTagHeatRows() {
        ensureFresh();
        List<Map<String, Object>> src = cachedAllRows;
        if (src == null || src.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> m : src) {
            copy.add(new LinkedHashMap<>(m));
        }
        return copy;
    }

    @Override
    public long getLastComputedAtMillis() {
        return lastComputedAt;
    }

    private void ensureFresh() {
        if (System.currentTimeMillis() - lastComputedAt < CACHE_TTL_MS && !cachedWeights.isEmpty()) {
            return;
        }
        synchronized (lock) {
            if (System.currentTimeMillis() - lastComputedAt < CACHE_TTL_MS && !cachedWeights.isEmpty()) {
                return;
            }
            recompute();
        }
    }

    private void recompute() {
        Map<Integer, String> novelLabels = buildNovelLabelMap();
        Map<String, Double> raw = new HashMap<>();

        List<Integer> collectionNovelIds = safeList(userMapper.selectAllCollectionNovelIds());
        for (Integer nid : collectionNovelIds) {
            addLabelRaw(raw, novelLabels.get(nid), W_COLLECTION);
        }

        List<UserReadingHistory> histories = userReadingHistoryMapper.selectAllForSiteHeat();
        int maxDur = 1;
        if (histories != null) {
            for (UserReadingHistory h : histories) {
                if (h == null) continue;
                maxDur = Math.max(maxDur, safeInt(h.getReadDuration(), 0));
            }
            double logMax = Math.log1p(maxDur);
            for (UserReadingHistory h : histories) {
                if (h == null || h.getNovelId() == null) continue;
                double p = Math.min(Math.max(safeInt(h.getReadProgress(), 0), 0), 100) / 100.0;
                double durNorm = logMax > 0
                        ? Math.log1p(Math.max(safeInt(h.getReadDuration(), 0), 0)) / logMax
                        : 0.0;
                double add = W_READ_PROGRESS * p + W_READ_DURATION * durNorm;
                if (add > 0) {
                    addLabelRaw(raw, novelLabels.get(h.getNovelId()), add);
                }
            }
        }

        List<Map<String, Object>> commentRows = userCommentMapper.selectCommentCountByNovel();
        if (commentRows != null) {
            for (Map<String, Object> row : commentRows) {
                if (row == null) continue;
                Integer nid = parseInt(row.get("novelId"));
                if (nid == null) continue;
                Integer cObj = parseInt(row.get("cnt"));
                int c = cObj != null ? cObj : 0;
                int capped = Math.min(Math.max(c, 0), COMMENT_CAP);
                if (capped > 0) {
                    addLabelRaw(raw, novelLabels.get(nid), W_COMMENT_PER_NOVEL_UNIT * capped);
                }
            }
        }

        List<Integer> finishedIds = safeList(userFinishedNovelMapper.selectAllFinishedDistinctNovelIds());
        for (Integer nid : finishedIds) {
            addLabelRaw(raw, novelLabels.get(nid), W_FINISHED);
        }

        Map<String, Double> dbFallback = loadDbRecommendWeights();
        Map<String, Double> weights = buildWeightsAndTop5(raw, dbFallback);
        List<Map<String, Object>> top5 = buildTop5List(raw);
        for (Map<String, Object> row : top5) {
            Object nameObj = row.get("tagName");
            if (nameObj == null) continue;
            String name = nameObj.toString();
            row.put("computedRecommendWeight", weights.containsKey(name) ? round4(weights.get(name)) : null);
        }

        this.cachedWeights = weights;
        this.cachedTop5 = top5;
        this.cachedAllRows = buildAllTagRows(raw, weights, dbFallback);
        this.lastComputedAt = System.currentTimeMillis();
    }

    /** 系统标签表内每一行：热度、占比、当前推荐映射权重、库内兜底。 */
    private List<Map<String, Object>> buildAllTagRows(Map<String, Double> raw, Map<String, Double> weights, Map<String, Double> dbFallback) {
        List<Tag> sysTags = tagMapper.selectAll();
        if (sysTags == null || sysTags.isEmpty()) {
            return Collections.emptyList();
        }
        double sumAllRaw = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Tag t : sysTags) {
            if (t == null || t.getName() == null) {
                continue;
            }
            String name = t.getName().trim();
            if (name.isEmpty()) {
                continue;
            }
            double r = Math.max(0.0, raw.getOrDefault(name, 0.0));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tagId", t.getId());
            row.put("tagName", name);
            row.put("rawScore", round4(r));
            row.put("computedRecommendWeight", round4(weights.getOrDefault(name, dbFallback.getOrDefault(name, 0.1))));
            row.put("dbFallbackWeight", round4(dbFallback.getOrDefault(name, 0.1)));
            row.put("shareOfAllTagsPercent", sumAllRaw > 0 ? round2(100.0 * r / sumAllRaw) : 0.0);
            list.add(row);
        }
        list.sort((a, b) -> Double.compare(((Number) b.get("rawScore")).doubleValue(), ((Number) a.get("rawScore")).doubleValue()));
        int rank = 1;
        for (Map<String, Object> row : list) {
            row.put("rank", rank++);
        }
        return list;
    }

    private Map<Integer, String> buildNovelLabelMap() {
        Map<Integer, String> map = new HashMap<>();
        List<Map<String, Object>> novels = novelBookMainService.getAllNovels();
        if (novels == null) {
            return map;
        }
        for (Map<String, Object> row : novels) {
            if (row == null) continue;
            Integer id = parseInt(row.get("id"));
            if (id == null) continue;
            Object lab = row.get("label");
            if (lab != null) {
                map.put(id, lab.toString());
            }
        }
        return map;
    }

    private Map<String, Double> loadDbRecommendWeights() {
        Map<String, Double> map = new HashMap<>();
        List<Tag> tags = tagMapper.selectAll();
        if (tags == null) return map;
        for (Tag t : tags) {
            if (t == null || t.getName() == null || t.getRecommendWeight() == null) continue;
            String k = t.getName().trim();
            if (k.isEmpty()) continue;
            map.put(k, t.getRecommendWeight().doubleValue());
        }
        return map;
    }

    /**
     * 对「在 tag 表中出现过的标签」赋权重：有行为热度则映射到 [0.1,1.0]，否则用库里的 recommend_weight（仍作冷启动兜底）。
     */
    private Map<String, Double> buildWeightsAndTop5(Map<String, Double> raw, Map<String, Double> dbFallback) {
        List<Tag> sysTags = tagMapper.selectAll();
        if (sysTags == null || sysTags.isEmpty()) {
            return new HashMap<>(dbFallback);
        }

        double maxRaw = 0.0;
        for (Tag t : sysTags) {
            if (t == null || t.getName() == null) continue;
            String name = t.getName().trim();
            if (name.isEmpty()) continue;
            maxRaw = Math.max(maxRaw, raw.getOrDefault(name, 0.0));
        }

        Map<String, Double> out = new HashMap<>();
        if (maxRaw <= 0) {
            for (Tag t : sysTags) {
                if (t == null || t.getName() == null) continue;
                String k = t.getName().trim();
                if (k.isEmpty()) continue;
                double w = dbFallback.getOrDefault(k, 0.1);
                out.put(k, w);
            }
            return out;
        }

        for (Tag t : sysTags) {
            if (t == null || t.getName() == null) continue;
            String k = t.getName().trim();
            if (k.isEmpty()) continue;
            double r = Math.max(raw.getOrDefault(k, 0.0), 0.0);
            if (r <= 0) {
                out.put(k, dbFallback.getOrDefault(k, 0.1));
            } else {
                double norm = r / maxRaw;
                out.put(k, 0.1 + 0.9 * norm);
            }
        }
        return out;
    }

    private List<Map<String, Object>> buildTop5List(Map<String, Double> raw) {
        double sumAll = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Map.Entry<String, Double>> sorted = raw.entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().trim().isEmpty())
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Double> e : sorted) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("tagName", e.getKey());
            row.put("rawScore", round4(e.getValue()));
            double shareTotal = sumAll > 0 ? 100.0 * e.getValue() / sumAll : 0.0;
            row.put("shareOfAllTagsPercent", round2(shareTotal));
            list.add(row);
        }
        return list;
    }

    private static void addLabelRaw(Map<String, Double> raw, String label, double add) {
        if (label == null || label.trim().isEmpty() || add <= 0) return;
        String[] parts = label.split("[,，]");
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (t.isEmpty()) continue;
            raw.put(t, raw.getOrDefault(t, 0.0) + add);
        }
    }

    private static List<Integer> safeList(List<Integer> list) {
        return list != null ? list : Collections.emptyList();
    }

    private static int safeInt(Integer v, int def) {
        return v != null ? v : def;
    }

    private static Integer parseInt(Object o) {
        if (o == null) return null;
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
