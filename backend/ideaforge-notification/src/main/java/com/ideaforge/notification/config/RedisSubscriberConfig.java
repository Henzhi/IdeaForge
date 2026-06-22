package com.ideaforge.notification.config;

import com.ideaforge.notification.ws.StoryGenerationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
public class RedisSubscriberConfig {

    @Autowired
    private StoryGenerationWebSocketHandler webSocketHandler;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new ChunkMessageListener(), new PatternTopic("story:chunks:*"));
        return container;
    }

    private class ChunkMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = new String(message.getBody());
                String userId = extractField(body, "userId");
                if (userId != null) webSocketHandler.onChunkFromRedis(userId, body);
            } catch (Exception e) { log.warn("处理 Redis chunk 消息失败", e); }
        }

        private String extractField(String json, String field) {
            String key = "\"" + field + "\":";
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            int start = idx + key.length();
            while (start < json.length() && json.charAt(start) == ' ') start++;
            StringBuilder sb = new StringBuilder();
            while (start < json.length() && Character.isDigit(json.charAt(start))) { sb.append(json.charAt(start)); start++; }
            return sb.length() > 0 ? sb.toString() : null;
        }
    }
}
