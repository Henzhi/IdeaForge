package com.ideaforge.notification.ws;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事生成 WebSocket Handler(多副本安全版)。
 *
 * 设计要点(对应 IdeaForge详细设计.md 第八章):
 * - 同一用户多端登录 → userId -> Set<session>
 * - AI 服务流式 chunk 发布到 Redis Pub/Sub 频道 "story:chunks:{taskId}"
 * - 各副本订阅该频道,收到后查 task.userId 调用 onChunkFromRedis 转发本地 session
 * - 避免单机 Map 在多副本下丢失推送
 *
 * Sprint 3 完善:接入 RedisMessageListenerContainer 订阅 + TaskRepository 取 userId。
 */
@Slf4j
@Component
public class StoryGenerationWebSocketHandler extends TextWebSocketHandler {

    /** userId -> 该用户在本副本持有的所有 session(多端登录) */
    private final Map<String, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (IOException ignored) {}
            return;
        }
        String userId = loginId.toString();
        sessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 已连接: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String token = extractToken(session);
        if (token == null) return;
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) return;
        String userId = loginId.toString();
        Set<WebSocketSession> sessions = sessionMap.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) sessionMap.remove(userId);
        }
        log.info("WebSocket 已断开: userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 由 Redis Pub/Sub 监听器调用:收到 chunk 后只推给本副本持有的 session。
     * payload 为推送给客户端的完整 JSON 消息。
     */
    public void onChunkFromRedis(String userId, String payload) {
        Set<WebSocketSession> sessions = sessionMap.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(message);
                } catch (IOException e) {
                    log.warn("推送 WebSocket 消息失败: sessionId={}", s.getId(), e);
                }
            }
        }
    }

    /** 从握手 URL 参数 satoken=xxx 提取 token */
    private String extractToken(WebSocketSession session) {
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if ("satoken".equals(kv[0]) && kv.length == 2) {
                return kv[1];
            }
        }
        return null;
    }
}
