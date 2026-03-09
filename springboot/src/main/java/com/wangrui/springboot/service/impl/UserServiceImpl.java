package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.UserMapper;
import com.wangrui.springboot.pojo.User;
import com.wangrui.springboot.service.UserService;
import com.wangrui.springboot.util.JwtUtil;
import com.wangrui.springboot.util.SmsCodeStore;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("msg", "用户不存在");
            return result;
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 403);
            result.put("msg", "账号已被禁用");
            return result;
        }

        String stored = user.getPassword();
        boolean passwordOk = (stored != null && stored.startsWith("$2a$"))
            ? PASSWORD_ENCODER.matches(password, stored)
            : password.equals(stored);
        if (!passwordOk) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 402);
            result.put("msg", "密码错误");
            return result;
        }

        // 生成JWT token
        String token = JwtUtil.generateToken(user.getUsername(), user.getId(), user.getIsAdmin());
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        
        // 构建用户信息，不包含密码
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("isAdmin", user.getIsAdmin());
        userInfo.put("createdAt", user.getCreatedAt());
        System.out.println("构建的用户信息中的createdAt: " + userInfo.get("createdAt"));
        
        data.put("user", userInfo);
        
        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> register(String username, String password, String email) {
        User existUser = userMapper.findByUsername(username);
        if (existUser != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "用户名已存在");
            return result;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PASSWORD_ENCODER.encode(password));
        user.setEmail(email != null ? email : "");
        user.setPhone("");
        user.setIsAdmin(0);

        userMapper.insert(user);
        
        // 生成JWT token
        String token = JwtUtil.generateToken(user.getUsername(), user.getId(), user.getIsAdmin());
        
        // 构建返回结果，与登录接口保持一致的数据结构
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        
        // 构建用户信息，不包含密码
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("isAdmin", user.getIsAdmin());
        userInfo.put("createdAt", user.getCreatedAt());

        data.put("user", userInfo);
        
        result.put("code", 200);
        result.put("msg", "注册成功");
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> sendVerificationCode(String phone) {
        Map<String, Object> result = new HashMap<>();
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            result.put("code", 400);
            result.put("msg", "手机号格式不正确");
            return result;
        }
        String code = String.format("%06d", new Random().nextInt(1000000));
        SmsCodeStore.put(phone, code);
        System.out.println("[开发] 手机验证码 " + phone + " -> " + code);
        result.put("code", 200);
        result.put("msg", "验证码已发送");
        result.put("devCode", code); // 开发环境返回验证码，便于页面显示，生产环境可去掉
        return result;
    }

    @Override
    public Map<String, Object> loginByPhone(String phone, String code) {
        Map<String, Object> result = new HashMap<>();
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            result.put("code", 400);
            result.put("msg", "手机号格式不正确");
            return result;
        }
        if (!SmsCodeStore.verify(phone, code)) {
            result.put("code", 403);
            result.put("msg", "验证码错误或已过期");
            return result;
        }
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            result.put("code", 404);
            result.put("msg", "该手机号未注册");
            return result;
        }
        String token = JwtUtil.generateToken(user.getUsername(), user.getId(), user.getIsAdmin());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("isAdmin", user.getIsAdmin());
        userInfo.put("createdAt", user.getCreatedAt());
        data.put("user", userInfo);
        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", data);
        return result;
    }

    @Override
    public boolean addCollection(Integer userId, Integer novelId) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("novelId", novelId);
        userMapper.addCollection(params);
        return true;
    }

    @Override
    public boolean removeCollection(Integer userId, Integer novelId) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("novelId", novelId);
        userMapper.removeCollection(params);
        return true;
    }

    @Override
    public boolean checkCollection(Integer userId, Integer novelId) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("novelId", novelId);
        int count = userMapper.checkCollection(params);
        return count > 0;
    }

    @Override
    public List<Map<String, Object>> getCollectionList(Integer userId) {
        return userMapper.getCollectionList(userId);
    }

    @Override
    public Map<String, Object> updateUserInfo(Integer userId, String username, String email, String phone) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id", userId);
            params.put("username", username);
            params.put("email", email);
            params.put("phone", phone);
            
            userMapper.updateUserInfo(params);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("msg", "更新成功");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "更新失败");
            return result;
        }
    }
    
    @Override
    public Map<String, Object> getUserInfo(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("msg", "用户不存在");
            return result;
        }
        
        // 构建用户信息，不包含密码
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("isAdmin", user.getIsAdmin());
        userInfo.put("createdAt", user.getCreatedAt());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("user", userInfo);
        return result;
    }
}
