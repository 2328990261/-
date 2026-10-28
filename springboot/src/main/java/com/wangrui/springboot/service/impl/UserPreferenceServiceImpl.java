package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserPreferenceTagMapper;
import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.cache.RedisCacheService;
import com.wangrui.springboot.service.UserPreferenceService;
import com.wangrui.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** 用户偏好标签服务实现：根据收藏列表实时派生标签并支持自定义排序 */
@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    // 首页优先展示的第一排标签（固定顺序）
    private static final String[] FIRST_ROW_TAGS = {"日常", "奇幻", "校园", "冒险", "异世界"};
    // 第二梯队候选标签
    private static final String[] OTHER_TAGS = {
        "轻松", "搞笑", "治愈", "致郁", "甜宠", "热血", "恋爱", "成长",
        "智斗", "悬疑", "推理", "心理惊悚", "战斗", "竞技", "基建",
        "宫廷", "虚拟网游", "现实题材", "种田文", "转生", "穿越", "魔法",
        "全年龄", "轻百"
    };
    // 无足够收藏时的默认标签
    private static final String[] DEFAULT_TAGS = {"日常", "奇幻", "冒险", "异世界", "轻松"};

    // 用户偏好标签数据访问对象
    @Autowired
    private UserPreferenceTagMapper userPreferenceTagMapper;

    // 用户服务（用于读取收藏列表）
    @Autowired
    private UserService userService;
    // Redis缓存服务
    @Autowired
    private RedisCacheService redisCacheService;

    /** 获取用户全部偏好标签（收藏派生 + 自定义） */
    @Override
    public List<UserPreferenceTag> getUserPreferenceTags(Integer userId) {
        List<UserPreferenceTag> result = new ArrayList<>();
        // 1. 先取收藏派生标签
        result.addAll(getCollectionBasedTags(userId));
        // 2. 再取自定义标签
        result.addAll(userPreferenceTagMapper.selectByUserIdAndTagType(userId, "custom"));
        return result;
    }

    /** 按标签类型获取用户偏好标签 */
    @Override
    public List<UserPreferenceTag> getUserPreferenceTagsByType(Integer userId, String tagType) {
        // 收藏类型实时由收藏数据计算
        if ("collection".equals(tagType)) {
            return getCollectionBasedTags(userId);
        }
        return userPreferenceTagMapper.selectByUserIdAndTagType(userId, tagType);
    }

    /** 根据收藏列表实时派生用户偏好标签（取书屋前5个热门标签） */
    @Override
    public List<UserPreferenceTag> getCollectionBasedTags(Integer userId) {
        // 1. 读取收藏列表并统计各标签出现次数
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

        // 2. 分别筛选第一排标签与第二梯队标签并按频次降序
        List<String> firstRow = Arrays.stream(FIRST_ROW_TAGS)
            .filter(tagCount::containsKey)
            .sorted((a, b) -> Integer.compare(tagCount.get(b), tagCount.get(a)))
            .collect(Collectors.toList());
        List<String> other = Arrays.stream(OTHER_TAGS)
            .filter(t -> !Arrays.asList(FIRST_ROW_TAGS).contains(t) && tagCount.containsKey(t))
            .sorted((a, b) -> Integer.compare(tagCount.get(b), tagCount.get(a)))
            .collect(Collectors.toList());

        // 3. 组合为最多5个标签：第一排首位优先，其次第二梯队与第一排补充
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
        // 4. 不足5个时用默认标签补齐
        if (combined.size() < 5) {
            for (String t : DEFAULT_TAGS) {
                if (combined.size() >= 5) break;
                if (!combined.contains(t)) combined.add(t);
            }
        }

        // 5. 转换为偏好标签实体列表
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

    /** 保存用户自定义偏好标签，并在数据变更后失效推荐缓存 */
    @Override
    public void saveUserPreferenceTags(Integer userId, String tagType, List<UserPreferenceTag> tags) {
        // 收藏排序由收藏与阅读数据实时计算，不保存
        if ("collection".equals(tagType)) {
            return;
        }
        // 1. 去重并先删除旧数据
        List<UserPreferenceTag> toSave = dedupeTagsForSave(userId, tagType, tags);
        userPreferenceTagMapper.deleteByUserIdAndTagType(userId, tagType);
        if (!toSave.isEmpty()) {
            // 库表若对 (user_id, tag_name) 有唯一约束，历史上误写入的 collection 行会挡住同名 custom；按名再删一遍
            List<String> names = toSave.stream().map(UserPreferenceTag::getTagName).collect(Collectors.toList());
            userPreferenceTagMapper.deleteByUserIdAndTagNames(userId, names);
            userPreferenceTagMapper.insertBatch(toSave);
        }
        // 2. 失效该用户的推荐缓存
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

    /** 构建一条偏好标签实体记录 */
    private UserPreferenceTag buildTagRow(Integer userId, String tagType, String tagName, UserPreferenceTag src) {
        UserPreferenceTag x = new UserPreferenceTag();
        x.setUserId(userId);
        x.setTagName(tagName);
        x.setTagType(tagType);
        // 保留来源的首页展示标记
        Boolean fr = src.getIsFirstRow();
        x.setIsFirstRow(fr != null && fr);
        return x;
    }

    /** 删除单个偏好标签并失效推荐缓存 */
    @Override
    public boolean deleteUserPreferenceTag(Integer id) {
        try {
            // 1. 查询并删除偏好标签
            UserPreferenceTag preferenceTag = userPreferenceTagMapper.selectById(id);
            userPreferenceTagMapper.deleteById(id);
            // 2. 失效该作者用户的推荐缓存
            if (preferenceTag != null) {
                redisCacheService.incrementUserRecommendationVersion(preferenceTag.getUserId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 清空某用户的全部偏好标签并失效推荐缓存 */
    @Override
    public boolean deleteAllUserPreferenceTags(Integer userId) {
        try {
            // 1. 删除该用户的全部偏好标签
            userPreferenceTagMapper.deleteByUserId(userId);
            // 2. 失效该用户的推荐缓存
            redisCacheService.incrementUserRecommendationVersion(userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}