package com.wangrui.springboot.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wangrui.springboot.mapper.RecommendCFMapper;
import com.wangrui.springboot.mapper.RecommendCommentConfigMapper;
import com.wangrui.springboot.mapper.RecommendMainConfigMapper;
import com.wangrui.springboot.mapper.RecommendScoreDebugMapper;
import com.wangrui.springboot.mapper.UserCommentMapper;
import com.wangrui.springboot.mapper.UserFinishedNovelMapper;
import com.wangrui.springboot.mapper.UserReadingHistoryMapper;
import com.wangrui.springboot.mapper.UserRecommendProfileMapper;
import com.wangrui.springboot.pojo.RecommendCommentConfig;
import com.wangrui.springboot.pojo.RecommendMainConfig;
import com.wangrui.springboot.pojo.RecommendScoreDebug;
import com.wangrui.springboot.pojo.UserComment;
import com.wangrui.springboot.pojo.UserFinishedNovel;
import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.pojo.UserRecommendProfile;
import com.wangrui.springboot.pojo.UserDislikeSnapshot;
import com.wangrui.springboot.pojo.vo.RecommendBooksResponse;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.RecommendMixService;
import com.wangrui.springboot.service.UserDislikeService;
import com.wangrui.springboot.service.UserEffectiveTagWeightService;
import com.wangrui.springboot.service.UserPreferenceService;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.UserService;
import com.wangrui.springboot.util.CacheEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class RecommendMixServiceImpl implements RecommendMixService {
    private static final Logger log = LoggerFactory.getLogger(RecommendMixServiceImpl.class);

    private static final double EPS = 1e-9;
    /** 候选池内按 S_mix 排名 ≤ 该名次的不打「换换口味」，避免前几名高分书被误标 */
    private static final int MMR_CHANGE_FLAVOR_POOL_MIX_RANK_THRESHOLD = 5;
    /** 最终推荐列表中按协同分取 Top N 展示「别人在看」 */
    private static final int OTHERS_READING_CF_TOP_N = 5;

    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private NovelBookMainService novelBookMainService;
    @Autowired
    private UserEffectiveTagWeightService userEffectiveTagWeightService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserDislikeService userDislikeService;
    @Autowired
    private UserFinishedNovelMapper userFinishedNovelMapper;
    @Autowired
    private UserReadingHistoryMapper userReadingHistoryMapper;
    @Autowired
    private UserCommentMapper userCommentMapper;
    @Autowired
    private RecommendCFMapper recommendCFMapper;
    @Autowired
    private RecommendMainConfigMapper recommendMainConfigMapper;
    @Autowired
    private RecommendCommentConfigMapper recommendCommentConfigMapper;
    @Autowired
    private UserRecommendProfileMapper userRecommendProfileMapper;
    @Autowired
    private RecommendScoreDebugMapper recommendScoreDebugMapper;
    @Autowired
    private RedisCacheService redisCacheService;
    @Autowired
    @Qualifier("recommendExecutor")
    private Executor recommendExecutor;

    @Override
    public RecommendBooksResponse recommend(Integer userId, String sortType) {
        long startedAt = System.currentTimeMillis();
        long recommendationVersion = redisCacheService.getUserRecommendationVersion(userId);
        String resultKey = recommendationVersion > 0
                ? "novel:rec:result:" + userId + ":" + sortType + ":" + recommendationVersion
                : null;
        if (resultKey != null) {
            CacheEntry<RecommendBooksResponse> cached = redisCacheService.getEntry(
                    resultKey, new TypeReference<CacheEntry<RecommendBooksResponse>>() {});
            if (cached != null && cached.getValue() != null) {
                RecommendBooksResponse value = cached.getValue();
                value.getDebug().put("cacheStatus", "HIT");
                value.getDebug().put("cachedAt", cached.getCachedAt() != null ? cached.getCachedAt().toString() : null);
                value.getDebug().put("elapsedMs", System.currentTimeMillis() - startedAt);
                log.info("recommendation request userId={} sortType={} cacheStatus=HIT elapsedMs={}",
                        userId, sortType, System.currentTimeMillis() - startedAt);
                return value;
            }
        }

        RecommendBooksResponse response = computeRecommendation(userId, sortType);
        String cacheStatus = resultKey != null ? "MISS" : "DISABLED_OR_UNAVAILABLE";
        long elapsedMs = System.currentTimeMillis() - startedAt;
        response.getDebug().put("cacheStatus", cacheStatus);
        response.getDebug().put("elapsedMs", elapsedMs);
        if (resultKey != null) {
            Instant cachedAt = Instant.now();
            response.getDebug().put("cachedAt", cachedAt.toString());
            redisCacheService.setEntry(resultKey, response,
                    redisCacheService.properties().getRecommendTtl());
        }
        log.info("recommendation request userId={} sortType={} cacheStatus={} elapsedMs={}",
                userId, sortType, cacheStatus, elapsedMs);
        return response;
    }

    private RecommendBooksResponse computeRecommendation(Integer userId, String sortType) {
        RecommendBooksResponse out = new RecommendBooksResponse();
        Map<String, Object> debug = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        out.setDebug(debug);
        debug.put("userId", userId);
        debug.put("sortType", sortType);

        List<UserPreferenceTag> userTags = "collection".equals(sortType)
            ? userPreferenceService.getCollectionBasedTags(userId)
            : userPreferenceService.getUserPreferenceTagsByType(userId, sortType);
        if (userTags == null || userTags.isEmpty()) {
            warnings.add("无用户偏好标签，无法推荐");
            debug.put("warnings", warnings);
            return out;
        }

        RecommendationData data = loadRecommendationData(userId, warnings);
        RecommendMainConfig mainCfg = data.mainConfig;
        RecommendCommentConfig behCfg = data.behaviorConfig;
        UserRecommendProfile profile = data.profile;

        Map<String, Double> personalWeights = data.personalWeights;
        final boolean isCustomSort = "custom".equals(sortType);
        // 收藏排序：仍用偏好顺序前 5 个键参与 tagHit；自定义排序：仅标签（+书名）模糊匹配得分，无协同/热度/行为/MMR，未命中任意勾选标签的书不进候选
        Map<String, Double> tagWeights = isCustomSort
            ? buildAllSelectedTagWeights(userTags, personalWeights)
            : buildTop5TagWeights(userTags, personalWeights);

        Set<Integer> blockedNovelIds = data.blockedNovelIds;
        Set<String> dislikedAuthorsLc = data.dislikedAuthorsLc;
        Set<String> dislikedTags = data.dislikedTags;

        Map<Integer, Integer> commentCntByNovel = data.commentCounts;
        Map<Integer, Integer> recentReadWindowCounts = data.recentReadCounts;
        Set<Integer> recentReadNovelIds = new HashSet<>(recentReadWindowCounts.keySet());
        RecommendScoreDebug scoreDebug = data.scoreDebug;
        double readRecordUnit = scoreDebug.getReadRecordUnitBonus() != null ? scoreDebug.getReadRecordUnitBonus().doubleValue() : 0.0;
        double cfCoocUnit = scoreDebug.getCfCoocUnitBonus() != null ? scoreDebug.getCfCoocUnitBonus().doubleValue() : 0.0;

        List<Map<String, Object>> all = data.allNovels != null ? data.allNovels : Collections.emptyList();

        List<Map<String, Object>> filtered = new ArrayList<>();
        /** 自定义排序：因与所选标签（含书名模糊）无任何匹配而未进入候选的数量 */
        int skippedCustomTag = 0;
        for (Map<String, Object> book : all) {
            if (book == null) continue;
            Integer nid = parseInt(book.get("id"));
            if (nid == null) continue;
            if (blockedNovelIds.contains(nid)) continue;
            if (isDislikedAuthor(book.get("author"), dislikedAuthorsLc)) continue;
            if (hasDislikedTag(book.get("label"), dislikedTags)) continue;
            filtered.add(book);
        }

        double popW = mainCfg.getPopWeight() != null ? mainCfg.getPopWeight().doubleValue() : 0.03;
        double cfLambda = mainCfg.getCfBlendLambda() != null ? mainCfg.getCfBlendLambda().doubleValue() : 0.25;
        int cfMaxSeeds = mainCfg.getCfMaxSeeds() != null ? mainCfg.getCfMaxSeeds() : 8;
        int cfPureCap = mainCfg.getCfPureSlotCap() != null ? mainCfg.getCfPureSlotCap() : 4;
        int mmrPoolCap = mainCfg.getMmrPoolCap() != null ? mainCfg.getMmrPoolCap() : 50;
        double mmrLambda = mainCfg.getMmrLambda() != null ? mainCfg.getMmrLambda().doubleValue() : 0.7;

        int recommendLimit = profile.getRecommendLimit() != null ? profile.getRecommendLimit() : 20;
        int topkSimilar = profile.getTopkSimilarPerSeed() != null ? profile.getTopkSimilarPerSeed() : 50;
        boolean cfOn = profile.getCfEnabled() != null && profile.getCfEnabled() == 1;
        boolean mmrOn = profile.getMmrEnabled() != null && profile.getMmrEnabled() == 1;
        double lambdaEff = cfOn ? clamp(cfLambda, 0, 1) : 0.0;
        if (isCustomSort) {
            lambdaEff = 0.0;
        }

        int maxRead = 0;
        for (Map<String, Object> book : filtered) {
            maxRead = Math.max(maxRead, parseReadCount(book));
        }
        if (maxRead <= 0) {
            maxRead = 1;
        }
        final double logDen = Math.log1p(maxRead);

        List<Candidate> cands = new ArrayList<>();
        for (Map<String, Object> book : filtered) {
            TagBreakdown tb = scoreBookBreakdown(book, tagWeights, isCustomSort);
            double tagHit = tb.total;
            if (isCustomSort) {
                if (tagHit <= EPS) {
                    skippedCustomTag++;
                    continue;
                }
            }
            int nid = parseInt(book.get("id"));
            int rc = parseReadCount(book);

            double commentB = 0;
            if (!isCustomSort
                && behCfg.getEnabled() != null && behCfg.getEnabled() == 1 && behCfg.getCommentWeight() != null) {
                int cap = behCfg.getCommentCountCap() != null ? behCfg.getCommentCountCap() : 50;
                int cnt = commentCntByNovel.getOrDefault(nid, 0);
                int capped = Math.min(Math.max(cnt, 0), cap);
                commentB = capped * behCfg.getCommentWeight().doubleValue();
            }
            double recentB = 0;
            if (!isCustomSort
                && behCfg.getReadHistoryEnabled() != null && behCfg.getReadHistoryEnabled() == 1
                && behCfg.getReadHistoryWeight() != null && recentReadNovelIds.contains(nid)) {
                recentB = behCfg.getReadHistoryWeight().doubleValue();
            }
            int readHits = recentReadWindowCounts.getOrDefault(nid, 0);
            double readDebugBonus = isCustomSort ? 0.0 : readHits * readRecordUnit;
            double behavior = commentB + recentB + readDebugBonus;
            double rawTag = tagHit + behavior;
            double popBoost = isCustomSort ? 0.0 : popW * (Math.log1p(rc) / logDen);

            Candidate c = new Candidate();
            c.book = book;
            c.novelId = nid;
            c.readCount = rc;
            c.tagHit = tagHit;
            c.commentBonus = commentB;
            c.recentReadBonus = recentB;
            c.readDebugBonus = readDebugBonus;
            c.behaviorBonus = behavior;
            c.rawTag = rawTag;
            c.popBoost = popBoost;
            c.tagBreakdown = tb;
            cands.add(c);
        }

        if (isCustomSort && cands.isEmpty()) {
            warnings.add("所选标签未匹配到任何作品（已对作品的标签与书名做模糊匹配），请尝试其他标签。");
        }

        Map<Integer, Double> cfScores;
        if (isCustomSort) {
            cfScores = Collections.emptyMap();
        } else {
            cfScores = computeCfScores(userId, cfOn, cfMaxSeeds, topkSimilar, cfCoocUnit, warnings);
        }
        for (Candidate c : cands) {
            c.cfScore = cfScores.getOrDefault(c.novelId, 0.0);
        }

        double maxRawTag = cands.stream().mapToDouble(x -> x.rawTag).max().orElse(0);
        double maxCf = cands.stream().mapToDouble(x -> x.cfScore).max().orElse(0);
        if (maxRawTag <= EPS) {
            maxRawTag = 1.0;
        }
        if (maxCf <= EPS) {
            maxCf = 1.0;
        }

        if (isCustomSort) {
            for (Candidate c : cands) {
                c.rawTag = c.tagHit;
                c.normTag = c.tagHit;
                c.normCf = 0.0;
                c.mix = c.tagHit;
            }
        } else {
            for (Candidate c : cands) {
                c.normTag = c.rawTag / maxRawTag;
                c.normCf = c.cfScore / maxCf;
                if (lambdaEff <= EPS) {
                    c.mix = c.rawTag + c.popBoost;
                } else {
                    // 在 rawTag 量级上混合：cf 先按本批 max 缩放到 [0, maxRawTag]，避免「双归一 + 凸组合」把综合分压在约 1 以内
                    double cfScaled = (maxCf <= EPS) ? 0.0 : (c.cfScore / maxCf) * maxRawTag;
                    c.mix = (1.0 - lambdaEff) * c.rawTag + lambdaEff * cfScaled + c.popBoost;
                }
            }
        }

        cands.sort((a, b) -> {
            int cmp = Double.compare(b.mix, a.mix);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a.novelId, b.novelId);
        });

        List<Candidate> pool = new ArrayList<>();
        for (Candidate c : cands) {
            if (pool.size() >= mmrPoolCap) break;
            pool.add(c);
        }

        List<Candidate> finalList;
        final boolean listFromMmr = !isCustomSort && mmrOn && !pool.isEmpty();
        Set<Integer> mmrDiversityPickIds = new HashSet<>();
        Map<Integer, Integer> poolMixRankByNovel = listFromMmr ? buildPoolMixRankByNovelId(pool) : Collections.emptyMap();
        if (listFromMmr) {
            finalList = mmrReorder(pool, recommendLimit, mmrLambda, cfPureCap, mmrDiversityPickIds, poolMixRankByNovel);
        } else {
            finalList = takeTopWithPureCap(cands, recommendLimit, cfPureCap);
        }

        Set<Integer> othersReadingIds = pickOthersReadingByTopCf(finalList, cfOn && !isCustomSort);

        List<Map<String, Object>> books = new ArrayList<>();
        for (Candidate c : finalList) {
            Map<String, Object> b = new LinkedHashMap<>(c.book);
            b.put("othersReading", othersReadingIds.contains(c.novelId));
            b.put("changeFlavor", !isCustomSort && listFromMmr && mmrDiversityPickIds.contains(c.novelId));
            books.add(b);
        }
        out.setBooks(books);

        debug.put("scoreDebugSnapshot", snapshotScoreDebug(scoreDebug));
        debug.put("warnings", warnings);
        debug.put("strategy", buildStrategyMap(isCustomSort, cfOn, mmrOn, recommendLimit, mmrPoolCap, lambdaEff, popW, mmrLambda));
        debug.put("formulas", buildFormulasText(lambdaEff, isCustomSort));
        debug.put("mainConfigSnapshot", snapshotMain(mainCfg));
        debug.put("behaviorConfigSnapshot", snapshotBehavior(behCfg));
        debug.put("profileSnapshot", snapshotProfile(profile));
        debug.put("tagVectorTop", buildTagVectorTop(personalWeights, 24));
        debug.put("top5PreferenceKeys", new ArrayList<>(tagWeights.keySet()));
        debug.put("tagFilterStats", buildTagFilterDebugStats(
            all.size(), skippedCustomTag, filtered, cands, tagWeights, personalWeights, sortType));
        debug.put("candidates", buildCandidateDebugRows(finalList, othersReadingIds, listFromMmr, mmrDiversityPickIds));

        return out;
    }

    private RecommendationData loadRecommendationData(Integer userId, List<String> warnings) {
        try {
            List<String> mainWarnings = new ArrayList<>();
            List<String> behaviorWarnings = new ArrayList<>();
            List<String> profileWarnings = new ArrayList<>();
            List<String> weightsWarnings = new ArrayList<>();
            List<String> blacklistWarnings = new ArrayList<>();
            List<String> commentWarnings = new ArrayList<>();
            List<String> recentWarnings = new ArrayList<>();
            List<String> scoreWarnings = new ArrayList<>();

            CompletableFuture<RecommendMainConfig> mainFuture =
                    CompletableFuture.supplyAsync(() -> loadMainConfig(mainWarnings), recommendExecutor);
            CompletableFuture<RecommendCommentConfig> behaviorFuture =
                    CompletableFuture.supplyAsync(() -> loadBehaviorConfig(behaviorWarnings), recommendExecutor);
            CompletableFuture<UserRecommendProfile> profileFuture =
                    CompletableFuture.supplyAsync(() -> loadProfile(userId, profileWarnings), recommendExecutor);
            CompletableFuture<Map<String, Double>> weightsFuture =
                    CompletableFuture.supplyAsync(() -> userEffectiveTagWeightService.effectiveWeightsByTagName(userId), recommendExecutor);
            CompletableFuture<RecommendationData.Blacklist> blacklistFuture =
                    CompletableFuture.supplyAsync(() -> {
                        RecommendationData.Blacklist blacklist = new RecommendationData.Blacklist();
                        loadBlacklist(userId, blacklist.blockedNovelIds, blacklist.dislikedAuthorsLc,
                                blacklist.dislikedTags, blacklistWarnings);
                        return blacklist;
                    }, recommendExecutor);
            CompletableFuture<Map<Integer, Integer>> commentsFuture =
                    CompletableFuture.supplyAsync(() -> loadCommentCounts(commentWarnings), recommendExecutor);
            CompletableFuture<Map<Integer, Integer>> recentFuture = behaviorFuture.thenCompose(behaviorConfig ->
                    CompletableFuture.supplyAsync(() -> loadRecentReadWindowCounts(userId, behaviorConfig, recentWarnings), recommendExecutor));
            CompletableFuture<RecommendScoreDebug> scoreFuture =
                    CompletableFuture.supplyAsync(() -> loadScoreDebug(scoreWarnings), recommendExecutor);
            CompletableFuture<List<Map<String, Object>>> novelsFuture =
                    CompletableFuture.supplyAsync(novelBookMainService::getAllNovels, recommendExecutor);

            CompletableFuture.allOf(mainFuture, behaviorFuture, profileFuture, weightsFuture,
                    blacklistFuture, commentsFuture, recentFuture, scoreFuture, novelsFuture)
                    .get(5, TimeUnit.SECONDS);

            RecommendationData data = new RecommendationData();
            data.mainConfig = mainFuture.get();
            data.behaviorConfig = behaviorFuture.get();
            data.profile = profileFuture.get();
            data.personalWeights = weightsFuture.get();
            RecommendationData.Blacklist blacklist = blacklistFuture.get();
            data.blockedNovelIds = blacklist.blockedNovelIds;
            data.dislikedAuthorsLc = blacklist.dislikedAuthorsLc;
            data.dislikedTags = blacklist.dislikedTags;
            data.commentCounts = commentsFuture.get();
            data.recentReadCounts = recentFuture.get();
            data.scoreDebug = scoreFuture.get();
            data.allNovels = novelsFuture.get();

            warnings.addAll(mainWarnings);
            warnings.addAll(behaviorWarnings);
            warnings.addAll(profileWarnings);
            warnings.addAll(blacklistWarnings);
            warnings.addAll(commentWarnings);
            warnings.addAll(recentWarnings);
            warnings.addAll(scoreWarnings);
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("recommendation parallel loading interrupted userId={} thread={}, falling back to sequential loading",
                    userId, Thread.currentThread().getName(), e);
            return loadRecommendationDataSequentially(userId, warnings);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("recommendation parallel loading failed userId={} thread={}, falling back to sequential loading",
                    userId, Thread.currentThread().getName(), e);
            return loadRecommendationDataSequentially(userId, warnings);
        } catch (RejectedExecutionException e) {
            log.warn("recommendation executor rejected parallel loading userId={} thread={}, falling back to sequential loading",
                    userId, Thread.currentThread().getName(), e);
            return loadRecommendationDataSequentially(userId, warnings);
        }
    }

    private RecommendationData loadRecommendationDataSequentially(Integer userId, List<String> warnings) {
        RecommendationData data = new RecommendationData();
        data.mainConfig = loadMainConfig(warnings);
        data.behaviorConfig = loadBehaviorConfig(warnings);
        data.profile = loadProfile(userId, warnings);
        data.personalWeights = userEffectiveTagWeightService.effectiveWeightsByTagName(userId);
        RecommendationData.Blacklist blacklist = new RecommendationData.Blacklist();
        loadBlacklist(userId, blacklist.blockedNovelIds, blacklist.dislikedAuthorsLc,
                blacklist.dislikedTags, warnings);
        data.blockedNovelIds = blacklist.blockedNovelIds;
        data.dislikedAuthorsLc = blacklist.dislikedAuthorsLc;
        data.dislikedTags = blacklist.dislikedTags;
        data.commentCounts = loadCommentCounts(warnings);
        data.recentReadCounts = loadRecentReadWindowCounts(userId, data.behaviorConfig, warnings);
        data.scoreDebug = loadScoreDebug(warnings);
        data.allNovels = novelBookMainService.getAllNovels();
        return data;
    }

    private static final class RecommendationData {
        private RecommendMainConfig mainConfig;
        private RecommendCommentConfig behaviorConfig;
        private UserRecommendProfile profile;
        private Map<String, Double> personalWeights;
        private Set<Integer> blockedNovelIds;
        private Set<String> dislikedAuthorsLc;
        private Set<String> dislikedTags;
        private Map<Integer, Integer> commentCounts;
        private Map<Integer, Integer> recentReadCounts;
        private RecommendScoreDebug scoreDebug;
        private List<Map<String, Object>> allNovels;

        private static final class Blacklist {
            private final Set<Integer> blockedNovelIds = new HashSet<>();
            private final Set<String> dislikedAuthorsLc = new HashSet<>();
            private final Set<String> dislikedTags = new HashSet<>();
        }
    }

    private Map<String, Object> buildStrategyMap(boolean isCustomSort, boolean cfOn, boolean mmrOn, int recLimit, int poolCap,
                                                 double lambdaEff, double popW, double mmrLambda) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (isCustomSort) {
            m.put("模式说明", "自定义排序：纯标签（含书名）模糊匹配，无阅读量热度、无协同、无行为加分、无 MMR");
            m.put("协同推荐开关", "关（本模式固定）");
            m.put("多样性重排_MMR", "关（本模式固定）");
        } else {
            m.put("协同推荐开关", cfOn ? "开" : "关");
            m.put("多样性重排_MMR", mmrOn ? "开" : "关");
        }
        m.put("每次推荐条数", recLimit);
        m.put("推荐池总数_MMR池上限", poolCap);
        m.put("协同混合λ_eff", lambdaEff);
        m.put("热度系数_pop_weight", popW);
        m.put("MMR平衡_mm_lambda", mmrLambda);
        return m;
    }

    private List<String> buildFormulasText(double lambdaEff, boolean isCustomSort) {
        List<String> lines = new ArrayList<>();
        if (isCustomSort) {
            lines.add("自定义排序：仅使用用户在个人中心勾选的偏好标签；对每本书的 label 分词及书名 bookMainName 与用户标签做 relaxedTokenMatch（全等或双向子串包含，较短串至少 2 字）；每个用户标签对同一本书至多计分一次。");
            lines.add("权重：与个人画像 UserEffectiveTagWeight 中该标签名一致则用画像权重；否则用画像最小权重的四分之一（下限 1e-6）。");
            lines.add("本模式不累加评论/阅读历史/调试阅读分，不计 popBoost（阅读量），不计算 Item-CF，不做 MMR；剔除未命中任意勾选标签（模糊）的书；综合分 S_mix = tagHit；同分按 novelId 升序稳定排序。");
            return lines;
        }
        lines.add("标签侧：收藏、个人阅读历史、个人评论等先在 UserEffectiveTagWeight 里按书的标签累计，归一成个人标签权重；本页 tagHit 使用「收藏/自定义排序得到的偏好标签顺序」的前 5 个标签名作为键，并从个人权重向量里取对应权重（与控制台「用户画像」按权重全局排序的前五行不一定相同）。");
        lines.add("tagHit：Top5 偏好标签键命中个人权重向量对应项之和。");
        lines.add("behavior：主推荐另叠加 recommend_comment_config — 全站评论封顶加分 + 最近窗口阅读记录次数×调试系数 +「最近读过」一次性加分（与上面标签画像统计是两条线）。");
        lines.add("rawTag = tagHit + behavior");
        lines.add("popBoost = pop_weight × (log(1+readCount) / log(1+maxRead))，maxRead 为本批过滤后候选的最大阅读量。");
        if (lambdaEff <= EPS) {
            lines.add("协同关闭：S_mix = rawTag + popBoost");
        } else {
            lines.add("协同开启：normTag=rawTag/maxRawTag，normCf=cfScore/maxCf（仅用于观察，不再直接代入综合分）");
            lines.add("cfScaled = (cfScore/maxCf)×maxRawTag，把协同分拉到与 rawTag 同一量级");
            lines.add("S_mix = (1-λ)·rawTag + λ·cfScaled + popBoost，λ 为 recommend_main_config.cf_blend_lambda");
        }
        lines.add("Item-CF：多种子书与用户互动共现，sim=coCount/sqrt(cntI*cntJ)，对候选累加 sim 为 cfScore；可在 recommend_score_debug 为每条共现边加 cf_cooc_unit_bonus。");
        lines.add("阅读调试分：最近窗口内同一本书出现几条阅读记录 × read_record_unit_bonus，叠加进 rawTag（与「最近读过」一次性加分可并存）。");
        lines.add("MMR：在高分池中用标签 Jaccard 做冗余惩罚；若某步在「明确放弃更高 S_mix」的前提下选了另一本，且该书在池内综合分排名在「第 "
            + (MMR_CHANGE_FLAVOR_POOL_MIX_RANK_THRESHOLD + 1) + " 名及以后」，则标「换换口味」（池内前 "
            + MMR_CHANGE_FLAVOR_POOL_MIX_RANK_THRESHOLD + " 名不因 MMR 与贪心基线不一致而打标）。");
        lines.add("纯协同名额：最终列表中 tagHit≈0 且 cfScore>0 的条数上限为 cf_pure_slot_cap。");
        lines.add("别人在看：在最终推荐列表中，仅 cfScore>0 的书参与比较，取协同分最高的至多 "
            + OTHERS_READING_CF_TOP_N + " 本展示该标签（不足则按实际数量）。");
        return lines;
    }

    /**
     * 从最终推荐结果中，取协同分最高的至多 {@link #OTHERS_READING_CF_TOP_N} 本用于展示「别人在看」。
     * 仅 cfScore&gt;0 的候选参与；同分按 novelId 升序稳定排序。
     */
    private static Set<Integer> pickOthersReadingByTopCf(List<Candidate> finalList, boolean cfOn) {
        if (!cfOn || finalList == null || finalList.isEmpty()) {
            return Collections.emptySet();
        }
        List<Candidate> eligible = new ArrayList<>();
        for (Candidate c : finalList) {
            if (c != null && c.cfScore > EPS) {
                eligible.add(c);
            }
        }
        eligible.sort((a, b) -> {
            int cmp = Double.compare(b.cfScore, a.cfScore);
            return cmp != 0 ? cmp : Integer.compare(a.novelId, b.novelId);
        });
        Set<Integer> ids = new LinkedHashSet<>();
        for (int i = 0; i < eligible.size() && i < OTHERS_READING_CF_TOP_N; i++) {
            ids.add(eligible.get(i).novelId);
        }
        return ids;
    }

    private List<Map<String, Object>> buildCandidateDebugRows(List<Candidate> finalList, Set<Integer> othersReadingIds,
                                                              boolean listFromMmr, Set<Integer> mmrDiversityPickIds) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (Candidate c : finalList) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("novelId", c.novelId);
            row.put("name", str(c.book.get("bookMainName")));
            row.put("tagHit", round4(c.tagHit));
            row.put("behavior", round4(c.behaviorBonus));
            row.put("commentBonus", round4(c.commentBonus));
            row.put("recentReadBonus", round4(c.recentReadBonus));
            row.put("readDebugBonus", round4(c.readDebugBonus));
            row.put("rawTag", round4(c.rawTag));
            row.put("cfScore", round4(c.cfScore));
            row.put("normTag", round4(c.normTag));
            row.put("normCf", round4(c.normCf));
            row.put("popBoost", round4(c.popBoost));
            row.put("S_mix", round4(c.mix));
            row.put("readCount", c.readCount);
            row.put("othersReading", othersReadingIds != null && othersReadingIds.contains(c.novelId));
            row.put("changeFlavor", listFromMmr && mmrDiversityPickIds != null && mmrDiversityPickIds.contains(c.novelId));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 标签筛选与 tagHit 打分对照：说明「用户画像权重表」与「实际参与打分的 Top5 键」是否一致，并统计各标签在候选中的覆盖。
     */
    private static Map<String, Object> buildTagFilterDebugStats(int allNovelsCount,
                                                               int skippedCustomTag,
                                                               List<Map<String, Object>> filtered,
                                                               List<Candidate> cands,
                                                               Map<String, Double> tagWeights,
                                                               Map<String, Double> personalWeights,
                                                               String sortType) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("tagFilterNote",
            "custom".equals(sortType)
                ? "自定义排序：在黑名单/不感兴趣过滤后，仅保留「label 或书名」与用户勾选标签至少一处模糊匹配的书；"
                + "tagHit 为勾选标签的宽松匹配得分之和（每标签每书至多一次）；不计阅读量、协同、行为分与 MMR。"
                : "tagHit 只累加 scoringTop5Weights 中的标签键在本书 label 上的命中；「用户画像」表是个人权重全局排序，"
                + "若与 scoringTop5Weights 的键集合不一致，请以 scoringTop5Weights 为准。"
                + "首页/书库按某几个标签筛书，与推荐里「收藏排序得到的偏好 Top5 键」可能不是同一组。");
        root.put("sortType", sortType);
        root.put("allNovelsCount", allNovelsCount);
        root.put("filteredAfterBlacklistCount", filtered != null ? filtered.size() : 0);
        root.put("customTagExcludedCount", skippedCustomTag);

        Map<String, Double> tw = tagWeights != null ? new LinkedHashMap<>(tagWeights) : new LinkedHashMap<>();
        root.put("scoringTop5Weights", tw);

        List<Map<String, Object>> globalTop5 = new ArrayList<>();
        Set<String> globalTop5Keys = new HashSet<>();
        if (personalWeights != null && !personalWeights.isEmpty()) {
            List<Map.Entry<String, Double>> es = new ArrayList<>(personalWeights.entrySet());
            es.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < Math.min(5, es.size()); i++) {
                Map.Entry<String, Double> e = es.get(i);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("tag", e.getKey());
                row.put("weight", round4(e.getValue()));
                globalTop5.add(row);
                globalTop5Keys.add(e.getKey());
            }
        }
        root.put("personalWeightsGlobalTop5Rows", globalTop5);

        Set<String> scoringKeys = new LinkedHashSet<>(tw.keySet());
        boolean sameKeySet = scoringKeys.size() == globalTop5Keys.size() && scoringKeys.containsAll(globalTop5Keys);
        root.put("scoringTop5KeySetEqualsPersonalWeightTop5KeySet", sameKeySet);

        Map<String, Integer> cover = new LinkedHashMap<>();
        for (String k : scoringKeys) {
            cover.put(k, 0);
        }
        int filteredHitAny = 0;
        if (filtered != null) {
            for (Map<String, Object> book : filtered) {
                Set<String> tokens = labelTokens(str(book.get("label")));
                boolean any = false;
                for (String k : scoringKeys) {
                    boolean hit = false;
                    if ("custom".equals(sortType)) {
                        for (String tok : tokens) {
                            if (relaxedTokenMatch(tok, k)) {
                                hit = true;
                                break;
                            }
                        }
                        if (!hit) {
                            String title = str(book.get("bookMainName"));
                            if (relaxedTokenMatch(title, k)) {
                                hit = true;
                            }
                        }
                    } else {
                        hit = tokens.contains(k);
                    }
                    if (hit) {
                        cover.merge(k, 1, Integer::sum);
                        any = true;
                    }
                }
                if (any) {
                    filteredHitAny++;
                }
            }
        }
        root.put("perScoringTagCoverInFiltered", cover);
        root.put("filteredBooksHitAnyScoringTagOnLabelCount", filteredHitAny);

        int pos = 0;
        int zero = 0;
        if (cands != null) {
            for (Candidate c : cands) {
                if (c.tagHit > EPS) {
                    pos++;
                } else {
                    zero++;
                }
            }
        }
        root.put("candidateCount", cands != null ? cands.size() : 0);
        root.put("candidateTagHitPositiveCount", pos);
        root.put("candidateTagHitZeroCount", zero);

        List<Map<String, Object>> samples = new ArrayList<>();
        if (cands != null) {
            for (Candidate c : cands) {
                if (c.tagHit <= EPS && samples.size() < 12) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("novelId", c.novelId);
                    row.put("bookMainName", str(c.book.get("bookMainName")));
                    row.put("labelRaw", str(c.book.get("label")));
                    samples.add(row);
                }
            }
        }
        root.put("sampleTagHitZeroBooks", samples);
        return root;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private List<Map<String, Object>> buildTagVectorTop(Map<String, Double> personalWeights, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (personalWeights == null || personalWeights.isEmpty()) return rows;
        List<Map.Entry<String, Double>> es = new ArrayList<>(personalWeights.entrySet());
        es.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        int n = Math.min(limit, es.size());
        for (int i = 0; i < n; i++) {
            Map.Entry<String, Double> e = es.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tag", e.getKey());
            row.put("weight", round4(e.getValue()));
            row.put("pct", Math.round(e.getValue() * 10000.0) / 100.0 + "%");
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> snapshotMain(RecommendMainConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c == null) return m;
        m.put("popWeight", c.getPopWeight());
        m.put("cfBlendLambda", c.getCfBlendLambda());
        m.put("cfMaxSeeds", c.getCfMaxSeeds());
        m.put("cfPureSlotCap", c.getCfPureSlotCap());
        m.put("mmrPoolCap", c.getMmrPoolCap());
        m.put("mmrLambda", c.getMmrLambda());
        return m;
    }

    private Map<String, Object> snapshotBehavior(RecommendCommentConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c == null) return m;
        m.put("enabled", c.getEnabled());
        m.put("commentWeight", c.getCommentWeight());
        m.put("commentCountCap", c.getCommentCountCap());
        m.put("readHistoryEnabled", c.getReadHistoryEnabled());
        m.put("readHistoryWeight", c.getReadHistoryWeight());
        m.put("readHistoryRecentLimit", c.getReadHistoryRecentLimit());
        return m;
    }

    private Map<String, Object> snapshotProfile(UserRecommendProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (p == null) return m;
        m.put("cfEnabled", p.getCfEnabled());
        m.put("mmrEnabled", p.getMmrEnabled());
        m.put("recommendLimit", p.getRecommendLimit());
        m.put("topkSimilarPerSeed", p.getTopkSimilarPerSeed());
        return m;
    }

    private RecommendMainConfig loadMainConfig(List<String> warnings) {
        String cacheKey = "novel:config:recommend-main";
        RecommendMainConfig cached = redisCacheService.get(cacheKey, RecommendMainConfig.class);
        if (cached != null) {
            return cached;
        }
        try {
            RecommendMainConfig c = recommendMainConfigMapper != null ? recommendMainConfigMapper.selectById(1) : null;
            if (c != null) {
                redisCacheService.set(cacheKey, c, redisCacheService.properties().getConfigTtl());
                return c;
            }
        } catch (Exception e) {
            warnings.add("recommend_main_config 读取失败，使用默认：" + e.getMessage());
        }
        RecommendMainConfig config = RecommendMainConfig.defaultConfig();
        redisCacheService.set(cacheKey, config, redisCacheService.properties().getConfigTtl());
        return config;
    }

    private RecommendCommentConfig loadBehaviorConfig(List<String> warnings) {
        String cacheKey = "novel:config:recommend-comment";
        RecommendCommentConfig cached = redisCacheService.get(cacheKey, RecommendCommentConfig.class);
        if (cached != null) {
            return cached;
        }
        try {
            RecommendCommentConfig c = recommendCommentConfigMapper != null ? recommendCommentConfigMapper.selectById(1) : null;
            if (c != null) {
                redisCacheService.set(cacheKey, c, redisCacheService.properties().getConfigTtl());
                return c;
            }
        } catch (Exception e) {
            warnings.add("recommend_comment_config 读取失败，使用默认：" + e.getMessage());
        }
        RecommendCommentConfig config = defaultBehaviorConfig();
        redisCacheService.set(cacheKey, config, redisCacheService.properties().getConfigTtl());
        return config;
    }

    private static RecommendCommentConfig defaultBehaviorConfig() {
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

    private UserRecommendProfile loadProfile(Integer userId, List<String> warnings) {
        try {
            UserRecommendProfile p = userRecommendProfileMapper != null ? userRecommendProfileMapper.selectByUserId(userId) : null;
            if (p != null) {
                return p;
            }
        } catch (Exception e) {
            warnings.add("user_recommend_profile 读取失败，使用默认：" + e.getMessage());
        }
        return UserRecommendProfile.defaultForUser(userId);
    }

    private void loadBlacklist(Integer userId, Set<Integer> blockedNovelIds, Set<String> dislikedAuthorsLc,
                               Set<String> dislikedTags, List<String> warnings) {
        try {
            List<Map<String, Object>> cols = userService.getCollectionList(userId);
            if (cols != null) {
                for (Map<String, Object> row : cols) {
                    Integer id = parseInt(row.get("id"));
                    if (id != null) {
                        blockedNovelIds.add(id);
                    }
                }
            }
        } catch (Exception e) {
            warnings.add("收藏列表读取失败（过滤可能不完整）：" + e.getMessage());
        }
        try {
            List<UserFinishedNovel> fins = userFinishedNovelMapper.selectByUserId(userId);
            if (fins != null) {
                for (UserFinishedNovel f : fins) {
                    if (f != null && f.getNovelId() != null) {
                        blockedNovelIds.add(f.getNovelId());
                    }
                }
            }
        } catch (Exception e) {
            warnings.add("完读列表读取失败：" + e.getMessage());
        }
        try {
            UserDislikeSnapshot d = userDislikeService.getDislike(userId);
            if (d != null) {
                if (d.getNovelIds() != null) {
                    blockedNovelIds.addAll(d.getNovelIds());
                }
                if (d.getAuthors() != null) {
                    for (String a : d.getAuthors()) {
                        if (a != null && !a.trim().isEmpty()) {
                            dislikedAuthorsLc.add(a.trim().toLowerCase(Locale.ROOT));
                        }
                    }
                }
                if (d.getTags() != null) {
                    for (String t : d.getTags()) {
                        if (t != null && !t.trim().isEmpty()) {
                            dislikedTags.add(t.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            warnings.add("不感兴趣读取失败：" + e.getMessage());
        }
    }

    private Map<Integer, Integer> loadCommentCounts(List<String> warnings) {
        Map<Integer, Integer> map = new HashMap<>();
        try {
            List<Map<String, Object>> rows = userCommentMapper.selectCommentCountByNovel();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    if (row == null) continue;
                    Integer nid = parseInt(row.get("novelId"));
                    if (nid == null) continue;
                    int cnt = 0;
                    Object cobj = row.get("cnt");
                    if (cobj != null) {
                        try {
                            cnt = Integer.parseInt(cobj.toString());
                        } catch (Exception ignored) {
                        }
                    }
                    map.put(nid, cnt);
                }
            }
        } catch (Exception e) {
            warnings.add("全站评论计数读取失败：" + e.getMessage());
        }
        return map;
    }

    private Map<Integer, Integer> loadRecentReadWindowCounts(Integer userId, RecommendCommentConfig beh, List<String> warnings) {
        Map<Integer, Integer> counts = new HashMap<>();
        try {
            int lim = beh.getReadHistoryRecentLimit() != null ? beh.getReadHistoryRecentLimit() : 5;
            List<UserReadingHistory> list = userReadingHistoryMapper.selectByUserId(userId);
            if (list == null) {
                return counts;
            }
            int n = Math.min(lim, list.size());
            for (int i = 0; i < n; i++) {
                UserReadingHistory h = list.get(i);
                if (h != null && h.getNovelId() != null) {
                    counts.merge(h.getNovelId(), 1, Integer::sum);
                }
            }
        } catch (Exception e) {
            warnings.add("阅读历史读取失败：" + e.getMessage());
        }
        return counts;
    }

    private RecommendScoreDebug loadScoreDebug(List<String> warnings) {
        try {
            RecommendScoreDebug row = recommendScoreDebugMapper != null ? recommendScoreDebugMapper.selectById(1) : null;
            if (row != null) {
                return row;
            }
        } catch (Exception e) {
            warnings.add("recommend_score_debug 读取失败，使用默认0：" + e.getMessage());
        }
        return RecommendScoreDebug.defaultRow();
    }

    private Map<String, Object> snapshotScoreDebug(RecommendScoreDebug d) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (d == null) {
            return m;
        }
        m.put("readRecordUnitBonus", d.getReadRecordUnitBonus());
        m.put("cfCoocUnitBonus", d.getCfCoocUnitBonus());
        return m;
    }

    private Map<Integer, Double> computeCfScores(Integer userId, boolean cfOn, int cfMaxSeeds, int topkSimilar,
                                                 double coocUnitBonus, List<String> warnings) {
        Map<Integer, Double> agg = new HashMap<>();
        if (!cfOn) {
            return agg;
        }
        List<Integer> seeds = buildSeeds(userId, cfMaxSeeds, warnings);
        if (seeds.isEmpty()) {
            return agg;
        }
        for (Integer seedId : seeds) {
            if (seedId == null) continue;
            try {
                List<Map<String, Object>> rows = recommendCFMapper.selectCooccurItems(seedId, topkSimilar, userId);
                if (rows == null) continue;
                for (Map<String, Object> row : rows) {
                    if (row == null) continue;
                    Integer j = parseInt(row.get("novelId"));
                    if (j == null || j.equals(seedId)) continue;
                    int co = parseIntOrZero(row.get("coCount"));
                    int cntI = parseIntOrZero(row.get("cntI"));
                    int cntJ = parseIntOrZero(row.get("cntJ"));
                    if (cntI <= 0 || cntJ <= 0) continue;
                    double sim = co / Math.sqrt((double) cntI * (double) cntJ);
                    double add = sim + coocUnitBonus;
                    agg.merge(j, add, Double::sum);
                }
            } catch (Exception e) {
                warnings.add("CF 共现查询失败 seed=" + seedId + "：" + e.getMessage());
            }
        }
        return agg;
    }

    private List<Integer> buildSeeds(Integer userId, int cfMaxSeeds, List<String> warnings) {
        Map<Integer, Integer> weight = new HashMap<>();
        try {
            List<Map<String, Object>> cols = userService.getCollectionList(userId);
            if (cols != null) {
                for (Map<String, Object> row : cols) {
                    Integer id = parseInt(row.get("id"));
                    if (id != null) {
                        weight.merge(id, 3, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            List<UserFinishedNovel> fins = userFinishedNovelMapper.selectByUserId(userId);
            if (fins != null) {
                for (UserFinishedNovel f : fins) {
                    if (f != null && f.getNovelId() != null) {
                        weight.merge(f.getNovelId(), 3, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            List<UserComment> comments = userCommentMapper.selectByUserId(userId);
            if (comments != null) {
                for (UserComment c : comments) {
                    if (c != null && c.getNovelId() != null) {
                        weight.merge(c.getNovelId(), 1, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            List<UserReadingHistory> hist = userReadingHistoryMapper.selectByUserId(userId);
            if (hist != null) {
                for (UserReadingHistory h : hist) {
                    if (h != null && h.getNovelId() != null) {
                        weight.merge(h.getNovelId(), 1, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        List<Integer> seeds = weight.entrySet().stream()
            .sorted((a, b) -> {
                int c = Integer.compare(b.getValue(), a.getValue());
                return c != 0 ? c : Integer.compare(a.getKey(), b.getKey());
            })
            .map(Map.Entry::getKey)
            .limit(cfMaxSeeds)
            .collect(Collectors.toList());

        if (!seeds.isEmpty()) {
            return seeds;
        }
        try {
            List<Map<String, Object>> top = recommendCFMapper.selectTopNovelIdsByInteractionCount(cfMaxSeeds);
            if (top != null) {
                for (Map<String, Object> row : top) {
                    Integer id = parseInt(row.get("novelId"));
                    if (id != null) {
                        seeds.add(id);
                    }
                }
            }
        } catch (Exception e) {
            warnings.add("CF 冷启动种子（全站热门）读取失败：" + e.getMessage());
        }
        return seeds;
    }

    private static List<Candidate> takeTopWithPureCap(List<Candidate> sorted, int limit, int pureCap) {
        List<Candidate> out = new ArrayList<>();
        int pure = 0;
        for (Candidate c : sorted) {
            if (out.size() >= limit) break;
            if (isPureCfSlot(c) && pure >= pureCap) continue;
            out.add(c);
            if (isPureCfSlot(c)) {
                pure++;
            }
        }
        return out;
    }

    private static boolean isPureCfSlot(Candidate c) {
        return c.tagHit <= EPS && c.cfScore > EPS;
    }

    /**
     * MMR 贪心重排；diversityFlavorNovelIds 记录「换换口味」：仅当某步明确选了 S_mix 更低的书，
     * 且该书在候选池内按 mix 排名在阈值之后（池内前几名不打标，避免列表头部误标）。
     */
    private List<Candidate> mmrReorder(List<Candidate> pool, int limit, double mmrLambda, int pureCap,
                                       Set<Integer> diversityFlavorNovelIds,
                                       Map<Integer, Integer> poolMixRankByNovelId) {
        if (pool.isEmpty()) {
            return pool;
        }
        if (diversityFlavorNovelIds == null) {
            diversityFlavorNovelIds = new HashSet<>();
        }
        Map<Integer, Integer> rankMap = poolMixRankByNovelId != null ? poolMixRankByNovelId : Collections.emptyMap();
        double maxMix = pool.stream().mapToDouble(c -> c.mix).max().orElse(1);
        if (maxMix <= EPS) {
            maxMix = 1.0;
        }
        List<Candidate> sortedPool = new ArrayList<>(pool);
        sortedPool.sort((a, b) -> Double.compare(b.mix, a.mix));

        List<Candidate> selected = new ArrayList<>();
        List<Candidate> remaining = new ArrayList<>(pool);

        Candidate first = null;
        int pure = 0;
        for (Candidate c : sortedPool) {
            if (isPureCfSlot(c) && pure >= pureCap) {
                continue;
            }
            first = c;
            if (isPureCfSlot(c)) {
                pure++;
            }
            break;
        }
        if (first == null) {
            first = sortedPool.get(0);
            if (isPureCfSlot(first)) {
                pure = 1;
            }
        }
        selected.add(first);
        remaining.remove(first);

        while (selected.size() < limit && !remaining.isEmpty()) {
            Candidate greedyMix = greedyMaxMixCandidate(remaining, pure, pureCap);

            double bestScore = Double.NEGATIVE_INFINITY;
            Candidate best = null;
            for (Candidate c : remaining) {
                if (isPureCfSlot(c) && pure >= pureCap) {
                    continue;
                }
                double rel = c.mix / maxMix;
                double maxSim = 0;
                Set<String> lc = labelTokens(str(c.book.get("label")));
                for (Candidate s : selected) {
                    maxSim = Math.max(maxSim, jaccard(lc, labelTokens(str(s.book.get("label")))));
                }
                double score = mmrLambda * rel - (1.0 - mmrLambda) * maxSim;
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
            if (best == null) {
                break;
            }
            if (greedyMix != null && best.novelId != greedyMix.novelId && greedyMix.mix > best.mix + EPS) {
                int poolRank = rankMap.getOrDefault(best.novelId, Integer.MAX_VALUE);
                if (poolRank > MMR_CHANGE_FLAVOR_POOL_MIX_RANK_THRESHOLD) {
                    diversityFlavorNovelIds.add(best.novelId);
                }
            }
            selected.add(best);
            remaining.remove(best);
            if (isPureCfSlot(best)) {
                pure++;
            }
        }
        return selected;
    }

    /** 当前剩余候选里综合分最高者（同分取 novelId 较小，便于与贪心基线对照） */
    private static Candidate greedyMaxMixCandidate(List<Candidate> remaining, int pure, int pureCap) {
        Candidate g = null;
        for (Candidate c : remaining) {
            if (isPureCfSlot(c) && pure >= pureCap) {
                continue;
            }
            if (g == null) {
                g = c;
            } else if (c.mix > g.mix + EPS) {
                g = c;
            } else if (Math.abs(c.mix - g.mix) <= EPS && c.novelId < g.novelId) {
                g = c;
            }
        }
        return g;
    }

    /** 候选池内按 S_mix 从高到低的名次（1 起算，同分 novelId 小者更靠前） */
    private static Map<Integer, Integer> buildPoolMixRankByNovelId(List<Candidate> pool) {
        Map<Integer, Integer> map = new HashMap<>();
        if (pool == null || pool.isEmpty()) {
            return map;
        }
        List<Candidate> copy = new ArrayList<>(pool);
        copy.sort((a, b) -> {
            int cmp = Double.compare(b.mix, a.mix);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a.novelId, b.novelId);
        });
        for (int i = 0; i < copy.size(); i++) {
            map.put(copy.get(i).novelId, i + 1);
        }
        return map;
    }

    private static Set<String> labelTokens(String label) {
        if (label == null || label.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> s = new HashSet<>();
        for (String p : label.split("[,，]")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                s.add(t);
            }
        }
        return s;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        int inter = 0;
        for (String x : a) {
            if (b.contains(x)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union <= 0 ? 0.0 : (double) inter / (double) union;
    }

    private static Map<String, Double> buildAllSelectedTagWeights(List<UserPreferenceTag> userTags,
                                                                 Map<String, Double> personalWeights) {
        Map<String, Double> tagWeights = new LinkedHashMap<>();
        if (userTags == null) {
            return tagWeights;
        }
        final int maxTags = 15;
        for (UserPreferenceTag tag : userTags) {
            if (tagWeights.size() >= maxTags) {
                break;
            }
            if (tag == null || tag.getTagName() == null) {
                continue;
            }
            String trimmed = tag.getTagName().trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            tagWeights.putIfAbsent(trimmed, weightForPreferenceTag(trimmed, personalWeights));
        }
        return tagWeights;
    }

    private static Map<String, Double> buildTop5TagWeights(List<UserPreferenceTag> userTags,
                                                          Map<String, Double> personalWeights) {
        Map<String, Double> tagWeights = new LinkedHashMap<>();
        for (int i = 0; i < userTags.size() && i < 5; i++) {
            UserPreferenceTag tag = userTags.get(i);
            if (tag.getTagName() == null) continue;
            String trimmed = tag.getTagName().trim();
            if (trimmed.isEmpty()) continue;
            tagWeights.put(trimmed, weightForPreferenceTag(trimmed, personalWeights));
        }
        return tagWeights;
    }

    private static double weightForPreferenceTag(String trimmedTag, Map<String, Double> personalWeights) {
        Double w = personalWeights.get(trimmedTag);
        if (w != null) {
            return w;
        }
        if (personalWeights.isEmpty()) {
            return 0.2;
        }
        double min = personalWeights.values().stream().mapToDouble(Double::doubleValue).min().orElse(1e-4);
        return Math.max(min * 0.25, 1e-6);
    }

    private static TagBreakdown scoreBookBreakdown(Map<String, Object> bookMap, Map<String, Double> tagWeights,
                                                   boolean customFuzzyScoring) {
        TagBreakdown bd = new TagBreakdown();
        Object labelObj = bookMap.get("label");
        String label = labelObj == null ? "" : labelObj.toString();
        if (!customFuzzyScoring) {
            if (label.isEmpty()) {
                return bd;
            }
            for (String bookTag : label.split("[,，]")) {
                String trimmedTag = bookTag.trim();
                if (trimmedTag.isEmpty()) {
                    continue;
                }
                if (tagWeights.containsKey(trimmedTag)) {
                    double w = tagWeights.get(trimmedTag);
                    bd.total += w;
                    bd.contributions.merge(trimmedTag, w, Double::sum);
                }
            }
            return bd;
        }
        Set<String> consumedUserKeys = new HashSet<>();
        if (!label.isEmpty()) {
            for (String bookTag : label.split("[,，]")) {
                String b = bookTag.trim();
                if (b.isEmpty()) {
                    continue;
                }
                for (Map.Entry<String, Double> e : tagWeights.entrySet()) {
                    String u = e.getKey();
                    if (consumedUserKeys.contains(u)) {
                        continue;
                    }
                    if (relaxedTokenMatch(b, u)) {
                        consumedUserKeys.add(u);
                        double w = e.getValue();
                        bd.total += w;
                        bd.contributions.merge(u, w, Double::sum);
                    }
                }
            }
        }
        String title = str(bookMap.get("bookMainName"));
        if (!title.isEmpty()) {
            for (Map.Entry<String, Double> e : tagWeights.entrySet()) {
                String u = e.getKey();
                if (consumedUserKeys.contains(u)) {
                    continue;
                }
                if (relaxedTokenMatch(title, u)) {
                    consumedUserKeys.add(u);
                    double w = e.getValue();
                    bd.total += w;
                    bd.contributions.merge(u, w, Double::sum);
                }
            }
        }
        return bd;
    }

    /**
     * 精确相等，或双向子串包含（较短串至少 2 字，减轻单字误配）。
     */
    private static boolean relaxedTokenMatch(String bookToken, String userKey) {
        if (bookToken == null || userKey == null) {
            return false;
        }
        String b = bookToken.trim();
        String u = userKey.trim();
        if (b.isEmpty() || u.isEmpty()) {
            return false;
        }
        if (b.equals(u)) {
            return true;
        }
        if (b.length() < 2 || u.length() < 2) {
            return false;
        }
        return b.contains(u) || u.contains(b);
    }

    private static boolean labelIntersectsAny(Object labelObj, Set<String> userTags) {
        if (labelObj == null) {
            return false;
        }
        Set<String> bookTags = labelTokens(labelObj.toString());
        for (String t : userTags) {
            if (bookTags.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDislikedTag(Object labelObj, Set<String> dislikedTags) {
        if (dislikedTags.isEmpty() || labelObj == null) {
            return false;
        }
        for (String bt : labelTokens(labelObj.toString())) {
            for (String dt : dislikedTags) {
                if (bt.equals(dt)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isDislikedAuthor(Object authorObj, Set<String> dislikedAuthorsLc) {
        if (dislikedAuthorsLc.isEmpty() || authorObj == null) {
            return false;
        }
        String a = authorObj.toString().trim().toLowerCase(Locale.ROOT);
        return dislikedAuthorsLc.contains(a);
    }

    private static Integer parseInt(Object o) {
        if (o == null) return null;
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseIntOrZero(Object o) {
        Integer v = parseInt(o);
        return v != null ? v : 0;
    }

    private static int parseReadCount(Map<String, Object> bookMap) {
        Object readCountObj = bookMap.get("readCount");
        if (readCountObj == null) return 0;
        try {
            return Integer.parseInt(readCountObj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static class TagBreakdown {
        double total;
        final Map<String, Double> contributions = new LinkedHashMap<>();
    }

    private static class Candidate {
        Map<String, Object> book;
        int novelId;
        int readCount;
        double tagHit;
        double commentBonus;
        double recentReadBonus;
        double readDebugBonus;
        double behaviorBonus;
        double rawTag;
        double cfScore;
        double normTag;
        double normCf;
        double popBoost;
        double mix;
        TagBreakdown tagBreakdown;
    }
}
