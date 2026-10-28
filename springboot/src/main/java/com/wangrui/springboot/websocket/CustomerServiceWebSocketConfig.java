package com.wangrui.springboot.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 客户服务 WebSocket 配置
 * 注册聊天处理器与握手拦截器，暴露 /ws/customer-service 通道
 */
@Configuration
@EnableWebSocket
public class CustomerServiceWebSocketConfig implements WebSocketConfigurer {

    // 聊天消息处理器
    private final CustomerServiceWebSocketHandler customerServiceWebSocketHandler;
    // 握手拦截器（身份校验）
    private final CustomerServiceHandshakeInterceptor customerServiceHandshakeInterceptor;

    // 构造方法：注入依赖
    public CustomerServiceWebSocketConfig(CustomerServiceWebSocketHandler customerServiceWebSocketHandler,
                                          CustomerServiceHandshakeInterceptor customerServiceHandshakeInterceptor) {
        this.customerServiceWebSocketHandler = customerServiceWebSocketHandler;
        this.customerServiceHandshakeInterceptor = customerServiceHandshakeInterceptor;
    }

    /**
     * 注册 WebSocket 处理器到指定路径，并绑定握手拦截器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 1. 注册处理器到 /ws/customer-service 路径
        registry.addHandler(customerServiceWebSocketHandler, "/ws/customer-service")
        // 2. 添加握手拦截器做身份校验
                .addInterceptors(customerServiceHandshakeInterceptor)
        // 3. 允许任意来源跨域连接
                .setAllowedOriginPatterns("*");
    }
}