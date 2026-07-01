package com.wangrui.springboot.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 原有：跨域配置（保持不变）
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有接口生效
                .allowedOrigins("http://localhost:5173") // 你的前端地址
                .allowedMethods("*") // 允许所有请求方法
                .allowedHeaders("*") // 允许所有请求头
                .exposedHeaders("*") // 暴露所有响应头
                .allowCredentials(true) // 允许携带Cookie
                .maxAge(3600); // 预检请求缓存时间（秒）
    }

    // 原有：静态资源映射（保持不变，确保封面图片能访问）
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    /**
     * JWT 校验：管理端、用户行为/偏好、推荐接口需携带有效 Token；
     * 管理接口额外要求 JWT 中 isAdmin = 1。
     * 登录注册在 /auth/**，小说公开接口在 /novel/**，不在此拦截范围内。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/api/admin/**", "/api/user/**", "/api/recommend/**")
                .addPathPatterns("/novel/detail/**", "/novel/chapter/**", "/novel/volume");

    }
}