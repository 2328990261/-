package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserPreferenceTagMapper;
import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.UserPreferenceService;
import com.wangrui.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final String[] FIRST_ROW_TAGS = {"日常", "奇幻", "校园", "冒险", "异世界"};
    private static final String[] OTHER_TAGS = {
        "轻松", "搞笑", "治愈", "致郁", "甜宠", "热血", "恋爱", "成长",
        "智斗", "悬疑", "推理", "心理惊悚", "战斗", "竞技", "基建",
        "宫廷", "虚拟网游", "现实题材", "种田文", "转生", "穿越", "魔法",
        "全年龄", "轻百"
    };
    private static final String[] DEFAULT_TAGS = {"日常", "奇幻", "冒险", "异世界", "轻松"};

    @Autowired
    private UserPreferenceTagMapper userPreferenceTagMapper;

    @Autowired
    private UserService userService;
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public List<UserPreferenceTag> getUserPreferenceTags(Integer userId) {
        List<UserPreferenceTag> result = new ArrayList<>();
        result.addAll(getCollectionBasedTags(userId));
        result.addAll(userPreferenceTagMapper.selectByUserIdAndTagType(userId, "custom"));
        return result;
    }

    @Override
    public List<UserPreferenceTag> getUserPreferenceTagsByType(Integer userId, String tagType) {
        if ("collection".equals(tagType)) {
            return getCollectionBasedTags(userId);
        }
        return userPreferenceTagMapper.selectByUserIdAndTagType(userId, tagType);
    }

    @Override
    public List<UserPreferenceTag> getCollectionBasedTags(Integer userId) {
        List<Map<String, Object>> books = userService.getCollectionList(userId);
        Map<String, Integer> tagCount = new HashMap<>();

        for (Map<String, Object> book : books) {
            Object labelObj = book.get("label");
            if (labelObj != null && !labelObj.toString().trim().isEmpty()) {
                String[] parts = labelObj.toString().split("[,，]");
                for (String t : parts) {
                    String tag = t.trim();
                    if (!tag.isEmpty()) {
                        tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
                    }
                }
            }
        }

        List<String> firstRow = Arrays.stream(FIRST_ROW_TAGS)
            .filter(tagCount::containsKey)
            .sorted((a, b) -> Integer.compare(tagCount.get(b), tagCount.get(a)))
            .collect(Collectors.toList());
        List<String> other = Arrays.stream(OTHER_TAGS)
            .filter(t -> !Arrays.asList(FIRST_ROW_TAGS).contains(t) && tagCount.containsKey(t))
            .sorted((a, b) -> Integer.compare(tagCount.get(b), tagCount.get(a)))
            .collect(Collectors.toList());

        List<String> combined = new ArrayList<>();
        if (!firstRow.isEmpty()) combined.add(firstRow.get(0));
        for (String t : other) {
            if (combined.size() >= 5) break;
            combined.add(t);
        }
        for (String t : firstRow) {
            if (combined.size() >= 5) break;
            if (!combined.contains(t)) combined.add(t);
        }
        if (combined.size() < 5) {
            for (String t : DEFAULT_TAGS) {
                if (combined.size() >= 5) break;
                if (!combined.contains(t)) combined.add(t);
            }
        }

        List<UserPreferenceTag> out = new ArrayList<>();
        for (int i = 0; i < combined.size(); i++) {
            UserPreferenceTag tag = new UserPreferenceTag();
            tag.setUserId(userId);
            tag.setTagName(combined.get(i));
            tag.setTagType("collection");
            tag.setTagOrder(i + 1);
            tag.setIsFirstRow(i < 5);
            out.add(tag);
        }
        return out;
    }

    @Override
    public void saveUserPreferenceTags(Integer userId, String tagType, List<UserPreferenceTag> tags) {
        if ("collection".equals(tagType)) {
            // 收藏排序由收藏与阅读数据实时计算，不保存
            return;
        }
        List<UserPreferenceTag> toSave = dedupeTagsForSave(userId, tagType, tags);
        userPreferenceTagMapper.deleteByUserIdAndTagType(userId, tagType);
        if (!toSave.isEmpty()) {
            // 库表若对 (user_id, tag_name) 有唯一约束，历史上误写入的 collection 行会挡住同名 custom；按名再删一遍
            List<String> names = toSave.stream().map(UserPreferenceTag::getTagName).collect(Collectors.toList());
            userPreferenceTagMapper.deleteByUserIdAndTagNames(userId, names);
            userPreferenceTagMapper.insertBatch(toSave);
        }
        redisCacheService.incrementUserRecommendationVersion(userId);
    }

    /** 按标签名字去重（保序），并统一 userId、tagType、tagOrder */
    private List<UserPreferenceTag> dedupeTagsForSave(Integer userId, String tagType, List<UserPreferenceTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserPreferenceTag> ordered = new LinkedHashMap<>();
        for (UserPreferenceTag t : tags) {
            if (t == null || t.getTagName() == null) {
                continue;
            }
            String name = t.getTagName().trim();
            if (name.isEmpty()) {
                continue;
            }
            ordered.putIfAbsent(name, buildTagRow(userId, tagType, name, t));
        }
        List<UserPreferenceTag> out = new ArrayList<>(ordered.values());
        for (int i = 0; i < out.size(); i++) {
            out.get(i).setTagOrder(i + 1);
        }
        return out;
    }

    private UserPreferenceTag buildTagRow(Integer userId, String tagType, String tagName, UserPreferenceTag src) {
        UserPreferenceTag x = new UserPreferenceTag();
        x.setUserId(userId);
        x.setTagName(tagName);
        x.setTagType(tagType);
        Boolean fr = src.getIsFirstRow();
        x.setIsFirstRow(fr != null && fr);
        return x;
    }

    @Override
    public boolean deleteUserPreferenceTag(Integer id) {
        try {
            UserPreferenceTag preferenceTag = userPreferenceTagMapper.selectById(id);
            userPreferenceTagMapper.deleteById(id);
            if (preferenceTag != null) {
                redisCacheService.incrementUserRecommendationVersion(preferenceTag.getUserId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteAllUserPreferenceTags(Integer userId) {
        try {
            userPreferenceTagMapper.deleteByUserId(userId);
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
