package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserPreferenceTagMapper;
import com.wangrui.springboot.pojo.UserPreferenceTag;
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
    public boolean saveUserPreferenceTags(Integer userId, String tagType, List<UserPreferenceTag> tags) {
        if ("collection".equals(tagType)) {
            // 收藏排序由收藏与阅读数据实时计算，不保存
            return true;
        }
        try {
            // 只删除指定类型的旧标签（仅 custom 会写入）
            userPreferenceTagMapper.deleteByUserIdAndTagType(userId, tagType);
            
            // 批量保存新的偏好标签
            if (tags != null && !tags.isEmpty()) {
                userPreferenceTagMapper.insertBatch(tags);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteUserPreferenceTag(Integer id) {
        try {
            userPreferenceTagMapper.deleteById(id);
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
