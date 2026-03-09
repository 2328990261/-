package com.wangrui.springboot.controller;

import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.service.TagService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminTagController {
    @Autowired
    private TagService tagService;

    @GetMapping("/tags")
    public Result<List<Tag>> listTags() {
        return Result.success(tagService.listAll());
    }

    @GetMapping("/tags/{id}")
    public Result<Tag> getTag(@PathVariable Integer id) {
        Tag tag = tagService.getById(id);
        return tag != null ? Result.success(tag) : Result.error("标签不存在");
    }

    @PostMapping("/tags")
    public Result<Integer> addTag(@RequestBody Tag tag) {
        if (tag.getName() == null || tag.getName().trim().isEmpty()) {
            return Result.error("标签名不能为空");
        }
        tag.setName(tag.getName().trim());
        if (tag.getRecommendWeight() == null) {
            tag.setRecommendWeight(java.math.BigDecimal.valueOf(0.1));
        }
        if (tag.getSortOrder() == null) {
            tag.setSortOrder(0);
        }
        tagService.add(tag);
        return Result.success(tag.getId());
    }

    @PutMapping("/tags/{id}")
    public Result<Void> updateTag(@PathVariable Integer id, @RequestBody Tag tag) {
        if (tagService.getById(id) == null) {
            return Result.error("标签不存在");
        }
        tag.setId(id);
        if (tag.getName() != null) tag.setName(tag.getName().trim());
        tagService.update(tag);
        return Result.success(null);
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@PathVariable Integer id) {
        if (tagService.getById(id) == null) {
            return Result.error("标签不存在");
        }
        tagService.delete(id);
        return Result.success(null);
    }

    /** 推荐配置：获取标签名->权重，供前端展示与推荐逻辑使用 */
    @GetMapping("/tags/weights")
    public Result<Map<String, Double>> getWeights() {
        return Result.success(tagService.getRecommendWeights());
    }
}
