package com.ideaforge.generation.consumer;

import com.ideaforge.generation.entity.GenerationTask;
import com.ideaforge.generation.repository.GenerationTaskRepository;
import com.ideaforge.generation.service.GenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 故事生成消费者(骨架)。
 * Sprint 3 接入 LangChain4j StreamingChatLanguageModel:
 * 1. 流式 chunk → Redis Pub/Sub 发布
 * 2. 通知模块订阅并推 WebSocket
 * 3. 全部完成后调用 StoryService.markCompleted 持久化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoryGenerationConsumer {

    private final GenerationTaskRepository taskRepository;

    @RabbitListener(queues = GenerationService.QUEUE)
    public void handle(Long taskId) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return;
        }

        task.setStatus("processing");
        task.setStartedAt(LocalDateTime.now());
        taskRepository.save(task);

        try {
            // TODO Sprint 3: 组装 prompt → 调用 LLM 流式生成 → Redis 发布 chunk
            // 完成后: storyService.markCompleted(storyId, fullContent, summary);
            log.info("生成任务处理完成(占位): taskId={}", taskId);
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        } catch (Exception e) {
            log.error("生成任务失败: taskId={}", taskId, e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }
}
