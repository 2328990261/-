package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    Map<String, Object> login(String username, String password);

    Map<String, Object> register(String username, String password, String email);

    /** 发送手机验证码（开发环境可仅打印到控制台） */
    Map<String, Object> sendVerificationCode(String phone);

    /** 手机号+验证码登录，成功返回与 login 一致的数据结构 */
    Map<String, Object> loginByPhone(String phone, String code);

    // 添加收藏
    boolean addCollection(Integer userId, Integer novelId);

    // 取消收藏
    boolean removeCollection(Integer userId, Integer novelId);

    // 检查收藏状态
    boolean checkCollection(Integer userId, Integer novelId);

    // 获取用户收藏列表
    List<Map<String, Object>> getCollectionList(Integer userId);

    // 更新用户信息
    Map<String, Object> updateUserInfo(Integer userId, String username, String email, String phone);
    
    // 获取用户信息
    Map<String, Object> getUserInfo(String username);
}
