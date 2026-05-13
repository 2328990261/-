package com.wangrui.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 【临时关闭 JWT】调试前端 401 等问题时取消下面拦截器注册；恢复校验请取消注释整块 addInterceptors。
    // @Autowired
    // private JwtInterceptor jwtInterceptor;

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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /* 【临时关闭 JWT 全局校验】调试完请恢复：取消本块注释，并恢复上方 @Autowired JwtInterceptor
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/auth/login",
                        "/auth/loginByPhone",
                        "/auth/register",
                        "/auth/sendCode",
                        "/novel/**",
                        "/api/recommend/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/images/**"
                );
        */
    }
}