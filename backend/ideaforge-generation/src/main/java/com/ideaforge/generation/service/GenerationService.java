package com.ideaforge.generation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideaforge.generation.entity.GenerationTask;
import com.ideaforge.generation.mapper.GenerationTaskMapper;
import com.ideaforge.idea.entity.Idea;
import com.ideaforge.idea.mapper.IdeaMapper;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成任务提交服务。
 * 流程: 查想法 → 组装 prompt → 创建 Story 草稿 → 创建任务(QUEUED) → 发送 RabbitMQ 消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    public static final String QUEUE = "story.generation.queue";
    /** Redis Pub/Sub 频道前缀: story:chunks:{taskId} */
    public static final String CHUNK_CHANNEL_PREFIX = "story:chunks:";

    private final GenerationTaskMapper generationTaskMapper;
    private final IdeaMapper ideaMapper;
    private final StoryService storyService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    @Transactional
    public Long submit(Long userId, List<Long> ideaIds, String style, String tone, String length) {
        // 1. 校验想法归属并查询内容
        List<Idea> ideas = ideaMapper.selectList(new LambdaQueryWrapper<Idea>()
                .eq(Idea::getUserId, userId)
                .in(Idea::getId, ideaIds)
                .isNull(Idea::getDeletedAt));
        if (ideas.isEmpty()) {
            throw new IllegalArgumentException("所选想法不存在或无权访问");
        }

        // 2. 组装 prompt
        String prompt = promptService.buildPrompt(ideas, style, tone, length);

        // 3. 创建 Story 草稿(标题取前几个想法摘要)
        String title = promptService.suggestTitle(ideas);
        Story story = storyService.createDraft(userId, title);

        // 4. 创建生成任务
        try {
            GenerationTask task = new GenerationTask();
            task.setUserId(userId);
            task.setStoryId(story.getId());
            task.setInputIdeas(objectMapper.writeValueAsString(ideaIds));
            task.setParameters(objectMapper.writeValueAsString(
                    Map.of("style", style == null ? "" : style,
                           "tone", tone == null ? "" : tone,
                           "length", length == null ? "medium" : length)));
            task.setPromptText(prompt);
            generationTaskMapper.insert(task);

            // 5. 发送 RabbitMQ 消息触发异步生成
            rabbitTemplate.convertAndSend(QUEUE, task.getId());
            log.info("生成任务已提交: taskId={}, storyId={}, ideaCount={}",
                    task.getId(), story.getId(), ideas.size());
            return task.getId();
        } catch (Exception e) {
            throw new RuntimeException("提交生成任务失败", e);
        }
    }

    /** 将 ideaIds(JSON 字符串)解析为 List<Long> */
    public List<Long> parseIdeaIds(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
