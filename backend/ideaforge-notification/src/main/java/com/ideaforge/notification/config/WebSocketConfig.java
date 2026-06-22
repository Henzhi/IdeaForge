package com.ideaforge.notification.config;

import com.ideaforge.notification.ws.StoryGenerationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 * 生产环境 setAllowedOrigins 限定来源,禁止 "*" 防止跨站 WebSocket 劫持。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /** 生产环境从配置读取允许的来源 */
    private static final String[] ALLOWED_ORIGINS = {
        "https://app.ideaforge.com",
        "https://ideaforge.com"
    };

    @Bean
    public StoryGenerationWebSocketHandler storyGenerationWebSocketHandler() {
        return new StoryGenerationWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(storyGenerationWebSocketHandler(), "/ws")
                .setAllowedOrigins(ALLOWED_ORIGINS);
    }
}
