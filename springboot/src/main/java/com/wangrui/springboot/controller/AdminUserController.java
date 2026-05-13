package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.service.AdminUserService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminUserController {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminUserService adminUserService;

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

    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUser(@PathVariable Integer id) {
        Map<String, Object> row = userMapper.selectUserAdminById(id);
        if (row == null || row.isEmpty()) {
            return Result.error("用户不存在");
        }
        return Result.success(row);
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> existing = userMapper.selectUserAdminById(id);
        if (existing == null || existing.isEmpty()) {
            return Result.error("用户不存在");
        }
        Object unameObj = body.get("username");
        if (unameObj == null || unameObj.toString().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        String username = unameObj.toString().trim();
        if (userMapper.countUsernameExcludeId(username, id) > 0) {
            return Result.error("用户名已存在");
        }

        Integer isAdmin = parseInt(body.get("isAdmin"));
        if (isAdmin == null || (isAdmin != 0 && isAdmin != 1)) {
            return Result.error("is_admin 须为 0 或 1");
        }
        Integer status = parseInt(body.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("status 须为 0(正常) 或 1(禁用)");
        }

        String email = normalizeNullableString(body.get("email"));
        String phone = normalizeNullableString(body.get("phone"));

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("username", username);
        params.put("email", email);
        params.put("phone", phone);
        params.put("isAdmin", isAdmin);
        params.put("status", status);

        Object pwObj = body.get("password");
        if (pwObj != null) {
            String pw = pwObj.toString().trim();
            if (!pw.isEmpty()) {
                params.put("passwordEncoded", PASSWORD_ENCODER.encode(pw));
            }
        }

        userMapper.adminUpdateUser(params);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("status 须为 0(正常) 或 1(禁用)");
        }
        userMapper.updateStatus(id, status);
        return Result.success(null);
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Integer id) {
        Map<String, Object> existing = userMapper.selectUserAdminById(id);
        if (existing == null || existing.isEmpty()) {
            return Result.error("用户不存在");
        }
        adminUserService.deleteUserAndRelations(id);
        return Result.success(null);
    }

    private static Integer parseInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeNullableString(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
