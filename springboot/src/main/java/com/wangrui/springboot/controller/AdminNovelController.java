package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminNovelController {
    @Autowired
    private NovelBookMainMapper novelBookMainMapper;

    /** 后台分页列表，status 可选：null=全部 0=下架 1=上架，支持标签和关键词筛选 */
    @GetMapping("/novels")
    public Result<Map<String, Object>> listNovels(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        int offset = (page - 1) * pageSize;
        List<NovelBook> list = novelBookMainMapper.selectNovelsByPageAdminWithFilter(offset, pageSize, status, tag, keyword);
        Integer total = novelBookMainMapper.selectNovelTotalCountAdminWithFilter(status, tag, keyword);
        List<Map<String, Object>> rows = list.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("bookMainName", n.getBookMainName());
            m.put("author", n.getAuthor());
            m.put("label", n.getLabel());
            m.put("cover", n.getCover());
            m.put("coverUrl", n.getCover() != null ? "/cover/" + n.getCover() : null);
            m.put("readCount", n.getReadCount());
            m.put("status", n.getStatus());
            m.put("createTime", n.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", total);
        return Result.success(data);
    }

    @GetMapping("/novels/{id}")
    public Result<Map<String, Object>> getNovel(@PathVariable Integer id) {
        NovelBook n = novelBookMainMapper.selectNovelById(id);
        if (n == null) return Result.error("书籍不存在");
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("bookMainName", n.getBookMainName());
        m.put("author", n.getAuthor());
        m.put("label", n.getLabel());
        m.put("cover", n.getCover());
        m.put("coverUrl", n.getCover() != null ? "/cover/" + n.getCover() : null);
        m.put("readCount", n.getReadCount());
        m.put("status", n.getStatus());
        m.put("createTime", n.getCreateTime());
        return Result.success(m);
    }

    @PutMapping("/novels/{id}")
    public Result<Void> updateNovel(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        NovelBook existing = novelBookMainMapper.selectNovelById(id);
        if (existing == null) return Result.error("书籍不存在");
        NovelBook n = new NovelBook();
        n.setId(id);
        n.setBookMainName(body.get("bookMainName") != null ? body.get("bookMainName").toString() : existing.getBookMainName());
        n.setAuthor(body.get("author") != null ? body.get("author").toString() : existing.getAuthor());
        n.setLabel(body.get("label") != null ? body.get("label").toString() : existing.getLabel());
        n.setCover(body.get("cover") != null ? body.get("cover").toString() : existing.getCover());
        n.setStatus(body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : existing.getStatus());
        novelBookMainMapper.updateNovel(n);
        return Result.success(null);
    }

    @PutMapping("/novels/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) return Result.error("status 须为 0 或 1");
        novelBookMainMapper.updateNovelStatus(id, status);
        return Result.success(null);
    }
}
