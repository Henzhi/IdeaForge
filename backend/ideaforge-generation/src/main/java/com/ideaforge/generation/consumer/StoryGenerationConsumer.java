package com.ideaforge.generation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideaforge.generation.entity.GenerationTask;
import com.ideaforge.generation.mapper.GenerationTaskMapper;
import com.ideaforge.generation.service.GenerationService;
import com.ideaforge.story.service.StoryService;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 故事生成消费者。
 * 流程:
 * 1. 从 DB 加载任务(含已组装的 prompt)
 * 2. 调用 LLM 流式生成,每个 chunk 发布到 Redis Pub/Sub 频道 story:chunks:{taskId}
 * 3. notification 模块订阅该频道,转发给对应用户的 WebSocket session
 * 4. 全部 token 生成完成后,拼装全文写入 Story 表并标记 completed
 *
 * 若 LLM 未配置(无 api-key),任务直接标记 failed。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoryGenerationConsumer {

    private final GenerationTaskMapper generationTaskMapper;
    private final StoryService storyService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 可选注入:未配置 api-key 时为 null */
    @Autowired(required = false)
    private OpenAiStreamingChatModel streamingChatModel;

    @RabbitListener(queues = GenerationService.QUEUE)
    public void handle(Long taskId) {
        GenerationTask task = generationTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return;
        }

        // 检查 LLM 是否可用
        if (streamingChatModel == null) {
            failTask(task, "LLM 未配置,请在 application.yml 设置 app.llm.api-key");
            return;
        }

        task.setStatus("processing");
        task.setStartedAt(LocalDateTime.now());
        generationTaskMapper.updateById(task);

        log.info("开始流式生成: taskId={}, storyId={}", taskId, task.getStoryId());

        StringBuilder fullContent = new StringBuilder();
        String channel = GenerationService.CHUNK_CHANNEL_PREFIX + taskId;

        try {
            streamingChatModel.generate(
                    List.of(UserMessage.from(task.getPromptText())),
                    new StreamingResponseHandler<>() {
                        @Override
                        public void onNext(String token) {
                            fullContent.append(token);
                            publishChunk(channel, task.getUserId(), taskId, token, "processing");
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            String content = fullContent.toString();
                            String summary = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                            // 持久化到 Story 表
                            storyService.markCompleted(task.getStoryId(), content, summary);
                            // 发布完成信号
                            publishChunk(channel, task.getUserId(), taskId, null, "completed");
                            // 更新任务状态
                            task.setStatus("completed");
                            task.setCompletedAt(LocalDateTime.now());
                            if (response.tokenUsage() != null) {
                                task.setTokensUsed((int) response.tokenUsage().totalTokenCount());
                            }
                            generationTaskMapper.updateById(task);
                            log.info("流式生成完成: taskId={}, 字数={}", taskId, content.length());
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("LLM 生成出错: taskId={}", taskId, error);
                            publishChunk(channel, task.getUserId(), taskId, null, "failed");
                            failTask(task, error.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("生成任务异常: taskId={}", taskId, e);
            publishChunk(channel, task.getUserId(), taskId, null, "failed");
            failTask(task, e.getMessage());
        }
    }

    /** 发布 chunk 到 Redis Pub/Sub,供 notification 模块订阅转发 WebSocket */
    private void publishChunk(String channel, Long userId, Long taskId, String chunk, String status) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "story_generation_chunk",
                    "userId", userId,
                    "taskId", taskId,
                    "chunk", chunk == null ? "" : chunk,
                    "status", status
            ));
            redisTemplate.convertAndSend(channel, payload);
        } catch (Exception e) {
            log.warn("发布 chunk 到 Redis 失败: taskId={}", taskId, e);
        }
    }

    private void failTask(GenerationTask task, String error) {
        task.setStatus("failed");
        task.setErrorMessage(error);
        task.setCompletedAt(LocalDateTime.now());
        generationTaskMapper.updateById(task);
    }
}
