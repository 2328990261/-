package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.CustomerServiceMessageMapper;
import com.wangrui.springboot.pojo.CustomerServiceMessage;
import com.wangrui.springboot.util.JwtUtil;
import com.wangrui.springboot.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Map;

@RestController
public class CustomerServiceMessageController {

    private final CustomerServiceMessageMapper messageMapper;

    public CustomerServiceMessageController(CustomerServiceMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @GetMapping("/api/customer-service/messages")
    public Result<List<CustomerServiceMessage>> getMyMessages(HttpServletRequest request) {
        Integer userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }

        List<CustomerServiceMessage> messages = messageMapper.selectByUserId(userId);
        messageMapper.markServiceMessagesRead(userId);
        return Result.success(messages);
    }

    @GetMapping("/api/admin/customer-service/conversations")
    public Result<List<Map<String, Object>>> getConversations() {
        return Result.success(messageMapper.selectConversations());
    }

    @GetMapping("/api/admin/customer-service/messages/{userId}")
    public Result<List<CustomerServiceMessage>> getUserMessages(@PathVariable Integer userId) {
        if (userId == null || userId <= 0) {
            return Result.error(400, "用户ID无效");
        }

        List<CustomerServiceMessage> messages = messageMapper.selectByUserId(userId);
        messageMapper.markUserMessagesRead(userId);
        return Result.success(messages);
    }

    @PutMapping("/api/admin/customer-service/messages/{userId}/read")
    public Result<Void> markUserMessagesRead(@PathVariable Integer userId) {
        if (userId == null || userId <= 0) {
            return Result.error(400, "用户ID无效");
        }
        messageMapper.markUserMessagesRead(userId);
        return Result.success();
    }

    private Integer getUserIdFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7).trim();
        return JwtUtil.validateToken(token) ? JwtUtil.getUserIdFromToken(token) : null;
    }
}
