package com.example.edog.configurer;

import com.example.edog.service.WebSocketServer;
import com.example.edog.utils.OrderWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private OrderWebSocketHandler orderWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // ESP32 连接的端点
        registry.addHandler(webSocketServer, "/esp32").setAllowedOrigins("*");

        // 设备命令连接的端点
        registry.addHandler(orderWebSocketHandler, "/order").setAllowedOrigins("*");
    }
}

