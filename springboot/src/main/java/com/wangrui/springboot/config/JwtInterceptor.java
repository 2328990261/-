package com.wangrui.springboot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.util.JwtUtil;
import com.wangrui.springboot.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 校验请求头 Authorization: Bearer token。
 * /api/admin/** 额外要求 JWT 中 isAdmin = 1。
 */

public class JwtInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static void writeJson(HttpServletResponse response, int httpStatus, Result<?> body) throws Exception {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            writeJson(response, 401, Result.error(401, "未登录或缺少 Token"));
            return false;
        }
        if (!JwtUtil.validateToken(token)) {
            writeJson(response, 401, Result.error(401, "登录已过期或 Token 无效"));
            return false;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin")) {
            try {
                Integer isAdmin = JwtUtil.getIsAdminFromToken(token);
                if (isAdmin == null || isAdmin != 1) {
                    writeJson(response, 403, Result.error(403, "需要管理员权限"));
                    return false;
                }
            } catch (Exception e) {
                writeJson(response, 403, Result.error(403, "需要管理员权限"));
                return false;
            }
        }

        return true;
    }
}
