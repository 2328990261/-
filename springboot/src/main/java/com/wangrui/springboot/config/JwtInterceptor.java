package com.wangrui.springboot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.util.JwtUtil;
import com.wangrui.springboot.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 校验 Authorization Bearer 令牌，通过后写入 request 属性供后续使用。
 * 管理端路径额外校验 token 中的 isAdmin。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "jwtUserId";
    public static final String ATTR_USERNAME = "jwtUsername";
    public static final String ATTR_IS_ADMIN = "jwtIsAdmin";

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;

    public JwtInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            writeUnauthorized(response, "未授权：请登录后访问");
            return false;
        }

        String token = auth.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || !JwtUtil.validateToken(token)) {
            writeUnauthorized(response, "未授权或登录已过期");
            return false;
        }

        Integer userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);
        Integer isAdmin = JwtUtil.getIsAdminFromToken(token);

        if (userId == null || username == null) {
            writeUnauthorized(response, "令牌无效");
            return false;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith(request.getContextPath() + "/api/admin")) {
            if (isAdmin == null || isAdmin != 1) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                Result<Object> body = Result.error(403, "需要管理员权限");
                response.getWriter().write(objectMapper.writeValueAsString(body));
                return false;
            }
        }

        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_USERNAME, username);
        request.setAttribute(ATTR_IS_ADMIN, isAdmin != null ? isAdmin : 0);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Object> body = Result.error(401, msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
