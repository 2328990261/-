package com.wangrui.springboot.controller;

import com.wangrui.springboot.service.UserService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /** 账号密码登录：用户名 3-20 位，密码 6-20 位 */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        if (username == null || username.trim().length() < 3 || username.trim().length() > 20) {
            return Result.error(400, "账号长度为 3-20 个字符");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return Result.error(400, "密码长度为 6-20 个字符");
        }
        Map<String, Object> result = userService.login(username.trim(), password);
        Integer code = (Integer) result.get("code");
        if (code == 200) {
            return Result.success(result.get("data"));
        } else {
            return Result.error(code, (String) result.get("msg"));
        }
    }

    /** 手机号验证码登录 */
    @PostMapping("/loginByPhone")
    public Result<Object> loginByPhone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.error(400, "请输入验证码");
        }
        Map<String, Object> result = userService.loginByPhone(phone, code.trim());
        Integer resCode = (Integer) result.get("code");
        if (resCode == 200) {
            return Result.success(result.get("data"));
        } else {
            return Result.error(resCode, (String) result.get("msg"));
        }
    }

    /** 发送手机验证码 */
    @PostMapping("/sendCode")
    public Result<Object> sendCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }
        Map<String, Object> result = userService.sendVerificationCode(phone);
        Integer code = (Integer) result.get("code");
        if (code == 200) {
            Map<String, Object> data = new HashMap<>();
            data.put("msg", result.get("msg"));
            data.put("devCode", result.get("devCode"));
            return Result.success(data);
        } else {
            return Result.error(code, (String) result.get("msg"));
        }
    }

    /** 注册：用户名 3-20 位，密码 6-20 位 */
    @PostMapping("/register")
    public Result<Object> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        if (username == null || username.trim().length() < 3 || username.trim().length() > 20) {
            return Result.error(400, "账号长度为 3-20 个字符");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return Result.error(400, "密码长度为 6-20 个字符");
        }
        Map<String, Object> result = userService.register(username.trim(), password, email != null ? email : "");
        Integer code = (Integer) result.get("code");
        if (code == 200) {
            Object data = result.get("data");
            return Result.success(data);
        } else {
            return Result.error(code, (String) result.get("msg"));
        }
    }

    @PostMapping("/updateUserInfo")
    public Result<Object> updateUserInfo(@RequestBody Map<String, Object> request) {
        Integer userId = (Integer) request.get("userId");
        String username = (String) request.get("username");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");

        Map<String, Object> result = userService.updateUserInfo(userId, username, email, phone);
        Integer code = (Integer) result.get("code");
        if (code == 200) {
            return Result.success(result);
        } else {
            return Result.error(code, (String) result.get("msg"));
        }
    }

    // 添加收藏
    @PostMapping("/collection/add")
    public Result<String> addCollection(@RequestBody Map<String, Integer> params) {
        Integer userId = params.get("userId");
        Integer novelId = params.get("novelId");
        
        if (userId == null || novelId == null) {
            return Result.fail("参数错误");
        }
        
        boolean result = userService.addCollection(userId, novelId);
        if (result) {
            return Result.success("收藏成功");
        } else {
            return Result.fail("收藏失败");
        }
    }

    // 取消收藏
    @PostMapping("/collection/remove")
    public Result<String> removeCollection(@RequestBody Map<String, Integer> params) {
        Integer userId = params.get("userId");
        Integer novelId = params.get("novelId");
        
        if (userId == null || novelId == null) {
            return Result.fail("参数错误");
        }
        
        boolean result = userService.removeCollection(userId, novelId);
        if (result) {
            return Result.success("取消收藏成功");
        } else {
            return Result.fail("取消收藏失败");
        }
    }

    // 检查收藏状态
    @GetMapping("/collection/check")
    public Result<Boolean> checkCollection(@RequestParam Integer userId, @RequestParam Integer novelId) {
        if (userId == null || novelId == null) {
            return Result.fail("参数错误");
        }
        
        boolean result = userService.checkCollection(userId, novelId);
        return Result.success(result);
    }

    // 获取用户收藏列表
    @GetMapping("/collection/list")
    public Result<List<Map<String, Object>>> getCollectionList(@RequestParam Integer userId) {
        if (userId == null) {
            return Result.fail("参数错误");
        }
        
        List<Map<String, Object>> result = userService.getCollectionList(userId);
        return Result.success(result);
    }
    
    // 获取用户信息
    @GetMapping("/userinfo")
    public Result<Object> getUserInfo(@RequestParam String username) {
        Map<String, Object> result = userService.getUserInfo(username);
        Integer code = (Integer) result.get("code");
        if (code == 200) {
            return Result.success(result.get("user"));
        } else {
            return Result.error(code, (String) result.get("msg"));
        }
    }
}
