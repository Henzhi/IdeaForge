package com.ideaforge.notification.config;

import com.ideaforge.notification.ws.StoryGenerationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 订阅配置。
 *
 * 监听 story:chunks:* 频道(模式订阅,匹配所有 taskId),
 * 收到消息后解析 userId,调用 WebSocketHandler 转发给该用户在本副本持有的 session。
 *
 * 这是多副本安全方案的核心:AI 服务发布 chunk 到 Redis,
 * 各副本订阅后只转发给本地 session,实现跨节点实时推送。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

    private final StoryGenerationWebSocketHandler webSocketHandler;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 模式订阅: story:chunks:* 匹配所有 taskId
        container.addMessageListener(new ChunkMessageListener(), new PatternTopic("story:chunks:*"));
        return container;
    }

    /**
     * chunk 消息监听器。
     * payload JSON 格式: {type, userId, taskId, chunk, status}
     * 提取 userId 后转发给 WebSocketHandler。
     */
    private class ChunkMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = new String(message.getBody());
                // 简单解析 userId(避免每条消息都反序列化整个 JSON)
                // 完整 payload 直接作为 WebSocket 消息转发
                String userId = extractField(body, "userId");
                if (userId != null) {
                    webSocketHandler.onChunkFromRedis(userId, body);
                }
            } catch (Exception e) {
                log.warn("处理 Redis chunk 消息失败", e);
            }
        }

        /** 从 JSON 字符串中提取指定字段的字符串值(轻量解析) */
        private String extractField(String json, String field) {
            String key = "\"" + field + "\":";
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            int start = idx + key.length();
            // 跳过可能的空格
            while (start < json.length() && json.charAt(start) == ' ') start++;
            // 数值型字段(userId 是数字)
            StringBuilder sb = new StringBuilder();
            while (start < json.length() && Character.isDigit(json.charAt(start))) {
                sb.append(json.charAt(start));
                start++;
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
    }
}
