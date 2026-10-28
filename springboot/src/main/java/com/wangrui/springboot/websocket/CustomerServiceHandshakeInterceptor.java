package com.wangrui.springboot.websocket;

import com.wangrui.springboot.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 客户服务 WebSocket 握手拦截器
 * 在建立连接前校验 token、识别用户/客服角色并写入会话属性
 */
@Component
public class CustomerServiceHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手前校验：通过 token 识别身份，校验客服权限，并写入角色信息
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // 1. 从请求地址中解析 token 和 role 参数
        var queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();
        String token = queryParams.getFirst("token");
        String role = queryParams.getFirst("role");

        // 2. 校验 token 是否存在且有效，无效则返回 401 拒绝连接
        if (!StringUtils.hasText(token) || !JwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            // 3. 解析 token 中的用户信息
            Integer userId = JwtUtil.getUserIdFromToken(token);
            Integer isAdmin = JwtUtil.getIsAdminFromToken(token);
            // 4. 判断是否为客服角色（service）
            boolean serviceRole = "service".equalsIgnoreCase(role);
            // 5. 客服必须具有管理员权限，否则拒绝
            if (serviceRole && (isAdmin == null || isAdmin != 1)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            // 6. 普通用户必须存在 userId
            if (!serviceRole && userId == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 7. 将身份信息写入会话属性，供后续处理器使用
            attributes.put("username", JwtUtil.getUsernameFromToken(token));
            attributes.put("userId", userId);
            attributes.put("isAdmin", isAdmin);
            attributes.put("role", serviceRole ? "SERVICE" : "USER");
        } catch (Exception exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        return true;
    }

    /**
     * 握手完成后回调，本场景无需额外处理
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后无需额外处理。
    }
}