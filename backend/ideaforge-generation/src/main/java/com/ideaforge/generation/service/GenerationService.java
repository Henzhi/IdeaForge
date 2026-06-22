package com.ideaforge.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideaforge.generation.entity.GenerationTask;
import com.ideaforge.generation.mapper.GenerationTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 生成任务提交服务。
 * 流程: 创建任务(QUEUED) → 发送 RabbitMQ 消息 → 消费者异步处理。
 * Sprint 3 接入 LangChain4j 流式生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    public static final String QUEUE = "story.generation.queue";

    private final GenerationTaskMapper generationTaskMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long submit(Long userId, List<Long> ideaIds, String style, String tone, String length) {
        try {
            GenerationTask task = new GenerationTask();
            task.setUserId(userId);
            task.setInputIdeas(objectMapper.writeValueAsString(ideaIds));
            task.setParameters(objectMapper.writeValueAsString(
                    Map.of("style", style, "tone", tone, "length", length)));
            generationTaskMapper.insert(task);

            rabbitTemplate.convertAndSend(QUEUE, task.getId());
            log.info("生成任务已提交: taskId={}, ideaCount={}", task.getId(), ideaIds.size());
            return task.getId();
        } catch (Exception e) {
            throw new RuntimeException("提交生成任务失败", e);
        }
    }
}
