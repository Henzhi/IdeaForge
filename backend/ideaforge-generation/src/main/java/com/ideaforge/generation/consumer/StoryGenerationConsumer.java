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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StoryGenerationConsumer {

    @Autowired
    private GenerationTaskMapper generationTaskMapper;

    @Autowired
    private StoryService storyService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private OpenAiStreamingChatModel streamingChatModel;

    @RabbitListener(queues = GenerationService.QUEUE)
    public void handle(Long taskId) {
        GenerationTask task = generationTaskMapper.selectById(taskId);
        if (task == null) { log.warn("任务不存在: {}", taskId); return; }

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
                        @Override public void onNext(String token) { fullContent.append(token); publishChunk(channel, task.getUserId(), taskId, token, "chunk"); }
                        @Override public void onComplete(Response<AiMessage> response) {
                            String content = fullContent.toString();
                            String summary = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                            storyService.markCompleted(task.getStoryId(), content, summary);
                            publishDone(channel, task.getUserId(), task.getStoryId(), content.length());
                            task.setStatus("completed");
                            task.setCompletedAt(LocalDateTime.now());
                            if (response.tokenUsage() != null) task.setTokensUsed((int) response.tokenUsage().totalTokenCount());
                            generationTaskMapper.updateById(task);
                            log.info("流式生成完成: taskId={}, 字数={}", taskId, content.length());
                        }
                        @Override public void onError(Throwable error) { log.error("LLM 生成出错: taskId={}", taskId, error); publishError(channel, task.getUserId(), error.getMessage()); failTask(task, error.getMessage()); }
                    });
        } catch (Exception e) {
            log.error("生成任务异常: taskId={}", taskId, e);
            publishChunk(channel, task.getUserId(), taskId, null, "failed");
            failTask(task, e.getMessage());
        }
    }

    /** 推送流式文本块到 Redis，格式与前端 WsMessage 对齐: {"type":"chunk","data":"..."} */
    private void publishChunk(String channel, Long userId, Long taskId, String chunk, String type) {
        try {
            if ("chunk".equals(type)) {
                redisTemplate.convertAndSend(channel,
                    objectMapper.writeValueAsString(Map.of("type", "chunk", "data", chunk == null ? "" : chunk, "userId", userId)));
            }
        } catch (Exception e) { log.warn("发布 chunk 到 Redis 失败: taskId={}", taskId, e); }
    }

    /** 推送完成事件: {"type":"done","storyId":...,"wordCount":...} */
    private void publishDone(String channel, Long userId, Long storyId, int wordCount) {
        try {
            redisTemplate.convertAndSend(channel,
                objectMapper.writeValueAsString(Map.of("type", "done", "storyId", storyId, "wordCount", wordCount, "userId", userId)));
        } catch (Exception e) { log.warn("发布 done 到 Redis 失败: storyId={}", storyId, e); }
    }

    /** 推送错误事件 */
    private void publishError(String channel, Long userId, String errorMsg) {
        try {
            redisTemplate.convertAndSend(channel,
                objectMapper.writeValueAsString(Map.of("type", "error", "message", errorMsg, "userId", userId)));
        } catch (Exception e) { log.warn("发布 error 到 Redis 失败", e); }
    }

    private void failTask(GenerationTask task, String error) {
        task.setStatus("failed"); task.setErrorMessage(error); task.setCompletedAt(LocalDateTime.now()); generationTaskMapper.updateById(task);
    }
}
