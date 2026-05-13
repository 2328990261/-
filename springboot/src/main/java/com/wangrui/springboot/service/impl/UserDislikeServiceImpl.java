package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.UserDislikeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.UserDislikeRequest;
import com.wangrui.springboot.pojo.UserDislikeSnapshot;
import com.wangrui.springboot.service.UserDislikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserDislikeServiceImpl implements UserDislikeService {

    @Autowired
    private UserDislikeMapper userDislikeMapper;

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;

    private static void splitLabels(String label, Set<String> out) {
        if (label == null || label.trim().isEmpty()) {
            return;
        }
        for (String part : label.split("[,，]")) {
            String t = part == null ? "" : part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
    }

    @Override
    public boolean saveDislike(Integer userId, UserDislikeRequest request) {
        if (userId == null || request == null || request.getNovelId() == null) {
            return false;
        }
        boolean blockNovel = Boolean.TRUE.equals(request.getBlockNovel());
        boolean blockAuthor = Boolean.TRUE.equals(request.getBlockAuthor());
        boolean allBookTags = Boolean.TRUE.equals(request.getAllBookTags());

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

        NovelBook book;
        try {
            book = novelBookMainMapper.selectNovelById(request.getNovelId());
        } catch (Exception e) {
            return false;
        }
        if (book == null) {
            return false;
        }

        if (allBookTags) {
            splitLabels(book.getLabel(), tagsToAdd);
        }

        if (!blockNovel && !blockAuthor && tagsToAdd.isEmpty()) {
            return false;
        }

        try {
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
            return true;
        } catch (BadSqlGrammarException e) {
            return false;
        }
    }

    @Override
    public UserDislikeSnapshot getDislike(Integer userId) {
        UserDislikeSnapshot snap = new UserDislikeSnapshot();
        if (userId == null) {
            return snap;
        }
        try {
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
