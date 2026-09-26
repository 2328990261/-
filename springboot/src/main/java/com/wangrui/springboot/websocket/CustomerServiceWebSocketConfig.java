package com.wangrui.springboot.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CustomerServiceWebSocketConfig implements WebSocketConfigurer {

    private final CustomerServiceWebSocketHandler customerServiceWebSocketHandler;
    private final CustomerServiceHandshakeInterceptor customerServiceHandshakeInterceptor;

    public CustomerServiceWebSocketConfig(CustomerServiceWebSocketHandler customerServiceWebSocketHandler,
                                          CustomerServiceHandshakeInterceptor customerServiceHandshakeInterceptor) {
        this.customerServiceWebSocketHandler = customerServiceWebSocketHandler;
        this.customerServiceHandshakeInterceptor = customerServiceHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(customerServiceWebSocketHandler, "/ws/customer-service")
                .addInterceptors(customerServiceHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
