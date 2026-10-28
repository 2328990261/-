package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.UserDislikeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.UserDislikeRequest;
import com.wangrui.springboot.pojo.UserDislikeSnapshot;
import com.wangrui.springboot.cache.RedisCacheService;
import com.wangrui.springboot.service.UserDislikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 用户不感兴趣服务实现：屏蔽小说/作者、标签降权，并失效推荐与热度缓存 */
@Service
public class UserDislikeServiceImpl implements UserDislikeService {

    // 用户不感兴趣数据访问对象
    @Autowired
    private UserDislikeMapper userDislikeMapper;

    // 小说主表数据访问对象
    @Autowired
    private NovelBookMainMapper novelBookMainMapper;
    // Redis缓存服务
    @Autowired
    private RedisCacheService redisCacheService;

    /** 按逗号拆分小说标签中的多个标签 */
    private static void splitLabels(String label, Set<String> out) {
        if (label == null || label.trim().isEmpty()) {
            return;
        }
        // 1. 按中文/英文逗号拆分并逐个去空格
        for (String part : label.split("[,，]")) {
            String t = part == null ? "" : part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
    }

    /** 保存用户不感兴趣设置：可屏蔽小说、作者与标签 */
    @Override
    public boolean saveDislike(Integer userId, UserDislikeRequest request) {
        // 1. 参数校验：用户、请求、小说ID任一为空则返回失败
        if (userId == null || request == null || request.getNovelId() == null) {
            return false;
        }
        // 2. 解析屏蔽维度
        boolean blockNovel = Boolean.TRUE.equals(request.getBlockNovel());
        boolean blockAuthor = Boolean.TRUE.equals(request.getBlockAuthor());
        boolean allBookTags = Boolean.TRUE.equals(request.getAllBookTags());

        // 3. 收集要屏蔽的标签名
        Set<String> tagsToAdd = new LinkedHashSet<>();
        if (request.getTagNames() != null) {
            for (String raw : request.getTagNames()) {
                if (raw == null) {
                    continue;
                }
                String t = raw.trim();
                if (!t.isEmpty()) {
                    tagsToAdd.add(t);
                }
            }
        }

        // 4. 校验小说是否存在
        NovelBook book;
        try {
            book = novelBookMainMapper.selectNovelById(request.getNovelId());
        } catch (Exception e) {
            return false;
        }
        if (book == null) {
            return false;
        }

        // 5. 需要屏蔽全部标签时，把小说标签拆解进待屏蔽集合
        if (allBookTags) {
            splitLabels(book.getLabel(), tagsToAdd);
        }

        // 6. 三个维度都为空则无需处理
        if (!blockNovel && !blockAuthor && tagsToAdd.isEmpty()) {
            return false;
        }

        try {
            // 7. 按维度写入屏蔽数据并失效推荐缓存
            if (blockNovel) {
                userDislikeMapper.insertNovel(userId, request.getNovelId());
            }
            if (blockAuthor && book.getAuthor() != null) {
                String a = book.getAuthor().trim();
                if (!a.isEmpty()) {
                    userDislikeMapper.insertAuthor(userId, a);
                }
            }
            for (String tag : tagsToAdd) {
                userDislikeMapper.insertTag(userId, tag);
            }
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (BadSqlGrammarException e) {
            return false;
        }
    }

    /** 查询用户已有的不感兴趣设置 */
    @Override
    public UserDislikeSnapshot getDislike(Integer userId) {
        UserDislikeSnapshot snap = new UserDislikeSnapshot();
        if (userId == null) {
            return snap;
        }
        try {
            // 1. 分别查询被屏蔽的小说、作者与标签
            List<Integer> nids = userDislikeMapper.selectNovelIdsByUserId(userId);
            List<String> authors = userDislikeMapper.selectAuthorsByUserId(userId);
            List<String> tags = userDislikeMapper.selectTagsByUserId(userId);
            snap.setNovelIds(nids != null ? nids : snap.getNovelIds());
            snap.setAuthors(authors != null ? authors : snap.getAuthors());
            snap.setTags(tags != null ? tags : snap.getTags());
        } catch (BadSqlGrammarException e) {
            // 表未建时返回空
        }
        return snap;
    }
}