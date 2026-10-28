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

/**
 * 后台用户管理控制器
 * 处理用户列表、详情、更新与级联删除等接口请求，路径前缀：/api/admin
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminUserController {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminUserService adminUserService;

    /**
     * 获取用户列表
     * @param page 页码，默认1页
     * @param pageSize
     * @param keyword
     * @param status
     * @param isAdmin
     * @return
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isAdmin) {
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> list = userMapper.selectUserPage(offset, pageSize, keyword, status, isAdmin);
        int total = userMapper.selectUserCount(keyword, status, isAdmin);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    /**
     * 获取用户详情
     * @param id
     * @return
     */
    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUser(@PathVariable Integer id) {
        Map<String, Object> row = userMapper.selectUserAdminById(id);
        if (row == null || row.isEmpty()) {
            return Result.error("用户不存在");
        }
        return Result.success(row);
    }

    /**
     * 更新用户信息
     * @param id
     * @param body
     * @return
     */
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

        // 构建更新参数
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

    /**
     * 更新用户状态
     * @param id
     * @param body
     * @return
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("status 须为 0(正常) 或 1(禁用)");
        }
        userMapper.updateStatus(id, status);
        return Result.success(null);
    }

    /**
     * 删除用户
     * @param id
     * @return
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Integer id) {
        Map<String, Object> existing = userMapper.selectUserAdminById(id);
        if (existing == null || existing.isEmpty()) {
            return Result.error("用户不存在");
        }
        adminUserService.deleteUserAndRelations(id);
        return Result.success(null);
    }

    /**
     * 将对象转换为整数
     * @param o
     * @return
     */
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

    /**
     * 将对象转换为非空字符串，移除首尾空格
     * @param o
     * @return
     */
    private static String normalizeNullableString(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}