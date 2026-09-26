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

@Component
public class CustomerServiceHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        var queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();
        String token = queryParams.getFirst("token");
        String role = queryParams.getFirst("role");

        if (!StringUtils.hasText(token) || !JwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Integer userId = JwtUtil.getUserIdFromToken(token);
            Integer isAdmin = JwtUtil.getIsAdminFromToken(token);
            boolean serviceRole = "service".equalsIgnoreCase(role);
            if (serviceRole && (isAdmin == null || isAdmin != 1)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            if (!serviceRole && userId == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

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

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后无需额外处理。
    }
}
