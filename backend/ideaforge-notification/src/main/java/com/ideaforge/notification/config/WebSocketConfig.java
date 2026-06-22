package com.ideaforge.notification.config;

import com.ideaforge.notification.ws.StoryGenerationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String[] ALLOWED_ORIGINS = { "https://app.ideaforge.com", "https://ideaforge.com" };

    @Autowired
    private StoryGenerationWebSocketHandler storyGenerationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(storyGenerationWebSocketHandler, "/ws").setAllowedOrigins(ALLOWED_ORIGINS);
    }
}
