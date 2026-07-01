package com.wangrui.springboot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendCFMapper {
    /**
     * 某本书的“互动用户数”（收藏/完读/评论/阅读历史任一算互动）。
     */
    Integer selectItemUserCount(@Param("novelId") Integer novelId);

    /**
     * 给定种子 novelId，找与其共现的其他 novel，返回 coCount/cntI/cntJ，用于余弦相似度：
     * sim(i,j)=coCount/sqrt(cntI*cntJ)
     *
     * 返回字段：
     * - novelId: 目标候选 j
     * - coCount: 共现用户数
     * - cntI: i 的互动用户数
     * - cntJ: j 的互动用户数
     *
     * @param excludeUserId 非空时从共现与 cntI/cntJ 中排除该用户，使协同分仅反映「其他用户」与 i、j 的共现（避免自共现堆分）
     */
    List<Map<String, Object>> selectCooccurItems(@Param("seedNovelId") Integer seedNovelId,
                                                  @Param("limit") Integer limit,
                                                  @Param("excludeUserId") Integer excludeUserId);

    /**
     * 全站互动人数最多的小说 ID（用于无用户行为时的全局物品协同冷启动种子）。
     */
    List<Map<String, Object>> selectTopNovelIdsByInteractionCount(@Param("limit") Integer limit);
}

