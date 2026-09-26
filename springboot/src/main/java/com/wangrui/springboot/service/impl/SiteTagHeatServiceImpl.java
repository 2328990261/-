package com.wangrui.springboot.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wangrui.springboot.mapper.TagMapper;
import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.SiteTagHeatService;
import com.wangrui.springboot.util.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class SiteTagHeatServiceImpl implements SiteTagHeatService {
    private static final Logger log = LoggerFactory.getLogger(SiteTagHeatServiceImpl.class);
    private static final String TAG_HEAT_KEY = "novel:stats:tag-heat";
    private static final String STALE_TAG_HEAT_KEY = "novel:stats:tag-heat:stale";

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
    @Autowired
    private RedisCacheService redisCacheService;
    @Autowired
    @Qualifier("statsExecutor")
    private Executor statsExecutor;

    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    @Override
    public Map<String, Double> getGlobalRecommendWeights() {
        return getSnapshot().weights;
    }

    @Override
    public List<Map<String, Object>> getTop5HotTags() {
        return getSnapshot().top5;
    }

    @Override
    public List<Map<String, Object>> getAllTagHeatRows() {
        return getSnapshot().allRows;
    }

    @Override
    public long getLastComputedAtMillis() {
        return getSnapshot().computedAt;
    }

    private TagHeatSnapshot getSnapshot() {
        CacheEntry<TagHeatSnapshot> fresh = redisCacheService.getEntry(
                TAG_HEAT_KEY, new TypeReference<CacheEntry<TagHeatSnapshot>>() {});
        if (fresh != null && fresh.getValue() != null) {
            return fresh.getValue();
        }

        CacheEntry<TagHeatSnapshot> stale = redisCacheService.getEntry(
                STALE_TAG_HEAT_KEY, new TypeReference<CacheEntry<TagHeatSnapshot>>() {});
        if (stale != null && stale.getValue() != null) {
            triggerAsyncRefresh();
            return stale.getValue();
        }

        return recomputeAndCache();
    }

    private void triggerAsyncRefresh() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            CompletableFuture.runAsync(this::refreshAndCache, statsExecutor)
                    .whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            log.warn("tag-heat async refresh failed thread={}",
                                    Thread.currentThread().getName(), throwable);
                        }
                        refreshInProgress.set(false);
                    });
        } catch (RejectedExecutionException e) {
            refreshInProgress.set(false);
            log.warn("tag-heat refresh rejected by stats executor thread={}",
                    Thread.currentThread().getName(), e);
            throw e;
        }
    }

    private TagHeatSnapshot refreshAndCache() {
        try {
            log.info("tag-heat recomputation started thread={}",
                    Thread.currentThread().getName());
            TagHeatSnapshot snapshot = recomputeAndCache();
            log.info("tag-heat recomputation completed thread={}",
                    Thread.currentThread().getName());
            return snapshot;
        } catch (Exception e) {
            log.warn("tag-heat recomputation failed thread={}",
                    Thread.currentThread().getName(), e);
            throw e;
        }
    }

    private TagHeatSnapshot recomputeAndCache() {
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

        List<Map<String, Object>> allRows = buildAllTagRows(raw, weights);

        TagHeatSnapshot snapshot = new TagHeatSnapshot();
        snapshot.weights = weights;
        snapshot.top5 = top5;
        snapshot.allRows = allRows;
        snapshot.computedAt = System.currentTimeMillis();

        redisCacheService.setEntry(TAG_HEAT_KEY, snapshot,
                redisCacheService.properties().getTagHeatTtl());
        redisCacheService.setEntry(STALE_TAG_HEAT_KEY, snapshot,
                redisCacheService.properties().getTagHeatTtl().multipliedBy(2));
        return snapshot;
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

    /**
     * 以 tag 表为准输出全部标签行：按原始热度降序；占比分母为全站 raw 总和（与 Top5 卡片一致）。
     */
    private List<Map<String, Object>> buildAllTagRows(Map<String, Double> raw, Map<String, Double> weights) {
        List<Tag> sysTags = tagMapper.selectAll();
        if (sysTags == null || sysTags.isEmpty()) {
            return Collections.emptyList();
        }
        double sumAll = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Tag> sorted = new ArrayList<>(sysTags);
        sorted.sort((a, b) -> {
            double ra = tagRaw(raw, a);
            double rb = tagRaw(raw, b);
            int c = Double.compare(rb, ra);
            if (c != 0) {
                return c;
            }
            String na = a.getName() != null ? a.getName() : "";
            String nb = b.getName() != null ? b.getName() : "";
            return na.compareToIgnoreCase(nb);
        });
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (Tag t : sorted) {
            if (t == null || t.getName() == null) {
                continue;
            }
            String name = t.getName().trim();
            if (name.isEmpty()) {
                continue;
            }
            double r = raw.getOrDefault(name, 0.0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("tagId", t.getId());
            row.put("tagName", name);
            row.put("rawScore", round4(r));
            row.put("computedRecommendWeight", weights.containsKey(name) ? round4(weights.get(name)) : null);
            row.put("shareOfAllTagsPercent", sumAll > 0 ? round2(100.0 * r / sumAll) : 0.0);
            list.add(row);
        }
        return list;
    }

    private static double tagRaw(Map<String, Double> raw, Tag t) {
        if (t == null || t.getName() == null) {
            return 0.0;
        }
        String k = t.getName().trim();
        return k.isEmpty() ? 0.0 : raw.getOrDefault(k, 0.0);
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

    public static final class TagHeatSnapshot {
        private Map<String, Double> weights = Collections.emptyMap();
        private List<Map<String, Object>> top5 = Collections.emptyList();
        private List<Map<String, Object>> allRows = Collections.emptyList();
        private long computedAt;

        public Map<String, Double> getWeights() {
            return weights;
        }

        public void setWeights(Map<String, Double> weights) {
            this.weights = weights != null ? weights : Collections.emptyMap();
        }

        public List<Map<String, Object>> getTop5() {
            return top5;
        }

        public void setTop5(List<Map<String, Object>> top5) {
            this.top5 = top5 != null ? top5 : Collections.emptyList();
        }

        public List<Map<String, Object>> getAllRows() {
            return allRows;
        }

        public void setAllRows(List<Map<String, Object>> allRows) {
            this.allRows = allRows != null ? allRows : Collections.emptyList();
        }

        public long getComputedAt() {
            return computedAt;
        }

        public void setComputedAt(long computedAt) {
            this.computedAt = computedAt;
        }
    }
}
