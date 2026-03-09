package com.wangrui.springboot.controller;

import com.wangrui.springboot.pojo.UserComment;
import com.wangrui.springboot.pojo.UserFinishedNovel;
import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.pojo.UserReadingHistory;
import com.wangrui.springboot.service.UserCommentService;
import com.wangrui.springboot.service.UserFinishedService;
import com.wangrui.springboot.service.UserPreferenceService;
import com.wangrui.springboot.service.UserReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户行为控制器
 * 处理用户偏好标签、阅读历史、完读记录和评论等相关接口请求
 * 路径前缀：/api/user/behavior
 */
@RestController
@RequestMapping("/api/user/behavior")
public class UserBehaviorController {

    @Autowired
    private UserPreferenceService userPreferenceService;

    @Autowired
    private UserReadingService userReadingService;

    @Autowired
    private UserFinishedService userFinishedService;

    @Autowired
    private UserCommentService userCommentService;

    // ==================== 用户偏好标签接口 ====================

    /**
     * 获取用户偏好标签
     * GET /api/user/behavior/preference/tags
     */
    @GetMapping("/preference/tags")
    public List<UserPreferenceTag> getUserPreferenceTags(@RequestParam("userId") Integer userId) {
        return userPreferenceService.getUserPreferenceTags(userId);
    }

    /**
     * 保存用户偏好标签
     * POST /api/user/behavior/preference/tags
     */
    @PostMapping("/preference/tags")
    public boolean saveUserPreferenceTags(
            @RequestParam("userId") Integer userId, 
            @RequestParam(value = "tagType", required = false, defaultValue = "collection") String tagType,
            @RequestBody List<UserPreferenceTag> tags) {
        return userPreferenceService.saveUserPreferenceTags(userId, tagType, tags);
    }

    /**
     * 删除用户偏好标签
     * DELETE /api/user/behavior/preference/tags/{id}
     */
    @DeleteMapping("/preference/tags/{id}")
    public boolean deleteUserPreferenceTag(@PathVariable("id") Integer id) {
        return userPreferenceService.deleteUserPreferenceTag(id);
    }

    // ==================== 用户阅读历史接口 ====================

    /**
     * 获取用户阅读历史
     * GET /api/user/behavior/reading/history
     */
    @GetMapping("/reading/history")
    public List<UserReadingHistory> getUserReadingHistory(@RequestParam("userId") Integer userId) {
        return userReadingService.getUserReadingHistory(userId);
    }

    /**
     * 获取用户所有小说阅读总时长（秒）
     * GET /api/user/behavior/reading/total-duration
     */
    @GetMapping("/reading/total-duration")
    public long getTotalReadDuration(@RequestParam("userId") Integer userId) {
        return userReadingService.getTotalReadDurationSeconds(userId);
    }

    /**
     * 保存用户阅读历史和时长
     * POST /api/user/behavior/reading/history
     */
    @PostMapping("/reading/history")
    public boolean saveUserReadingHistory(@RequestBody UserReadingHistory history) {
        return userReadingService.saveOrUpdateUserReadingHistory(history);
    }

    /**
     * 更新用户阅读历史
     * PUT /api/user/behavior/reading/history
     */
    @PutMapping("/reading/history")
    public boolean updateUserReadingHistory(@RequestBody UserReadingHistory history) {
        return userReadingService.saveOrUpdateUserReadingHistory(history);
    }

    // ==================== 用户完读记录接口 ====================

    /**
     * 获取用户完读记录
     * GET /api/user/behavior/finished/novels
     */
    @GetMapping("/finished/novels")
    public List<UserFinishedNovel> getUserFinishedNovels(@RequestParam("userId") Integer userId) {
        return userFinishedService.getUserFinishedNovels(userId);
    }

    /**
     * 标记小说为完读
     * POST /api/user/behavior/finished/novels
     */
    @PostMapping("/finished/novels")
    public boolean markNovelAsFinished(@RequestParam("userId") Integer userId, @RequestParam("novelId") Integer novelId) {
        return userFinishedService.markNovelAsFinished(userId, novelId);
    }

    /**
     * 取消完读记录
     * DELETE /api/user/behavior/finished/novels/{id}
     */
    @DeleteMapping("/finished/novels/{id}")
    public boolean cancelFinishedNovel(@PathVariable("id") Integer id) {
        return userFinishedService.cancelFinishedNovel(id);
    }

    /**
     * 取消指定小说的完读记录
     * DELETE /api/user/behavior/finished/novels
     */
    @DeleteMapping("/finished/novels")
    public boolean cancelFinishedNovelByNovelId(@RequestParam("userId") Integer userId, @RequestParam("novelId") Integer novelId) {
        return userFinishedService.cancelFinishedNovelByNovelId(userId, novelId);
    }

    // ==================== 用户评论接口 ====================

    /**
     * 获取用户评论
     * GET /api/user/behavior/comments
     */
    @GetMapping("/comments")
    public List<UserComment> getUserComments(@RequestParam("userId") Integer userId) {
        return userCommentService.getUserComments(userId);
    }

    /**
     * 获取小说的评论列表
     * GET /api/user/behavior/comments/novel
     */
    @GetMapping("/comments/novel")
    public List<UserComment> getNovelComments(@RequestParam("novelId") Integer novelId) {
        return userCommentService.getNovelComments(novelId);
    }

    /**
     * 发表评论
     * POST /api/user/behavior/comments
     */
    @PostMapping("/comments")
    public boolean addComment(@RequestBody UserComment comment) {
        return userCommentService.addComment(comment);
    }

    /**
     * 更新评论
     * PUT /api/user/behavior/comments/{id}
     */
    @PutMapping("/comments/{id}")
    public boolean updateComment(@PathVariable("id") Integer id, @RequestBody Map<String, String> request) {
        UserComment comment = userCommentService.getCommentById(id);
        if (comment != null) {
            comment.setContent(request.get("content"));
            return userCommentService.updateComment(comment);
        }
        return false;
    }

    /**
     * 删除评论
     * DELETE /api/user/behavior/comments/{id}
     */
    @DeleteMapping("/comments/{id}")
    public boolean deleteComment(@PathVariable("id") Integer id) {
        return userCommentService.deleteComment(id);
    }
}
