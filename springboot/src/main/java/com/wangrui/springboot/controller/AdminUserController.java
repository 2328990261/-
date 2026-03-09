package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminUserController {
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> list = userMapper.selectUserPage(offset, pageSize, keyword, status);
        int total = userMapper.selectUserCount(keyword, status);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) return Result.error("status 须为 0(正常) 或 1(禁用)");
        userMapper.updateStatus(id, status);
        return Result.success(null);
    }
}
