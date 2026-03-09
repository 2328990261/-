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

    // 新增：注册JWT拦截器（实现登录验证）
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new JwtlInterceptor())
//                .addPathPatterns("/api/novel/**", "/api/user/**", "/api/recommend/**")
//                .excludePathPatterns("/api/user/login/**", "/api/user/register", "/api/novel/label/**");
//    }
}