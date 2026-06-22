package com.ideaforge.generation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideaforge.generation.entity.GenerationTask;
import com.ideaforge.generation.mapper.GenerationTaskMapper;
import com.ideaforge.idea.entity.Idea;
import com.ideaforge.idea.mapper.IdeaMapper;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.service.StoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GenerationService {

    public static final String QUEUE = "story.generation.queue";
    public static final String CHUNK_CHANNEL_PREFIX = "story:chunks:";

    @Autowired
    private GenerationTaskMapper generationTaskMapper;

    @Autowired
    private IdeaMapper ideaMapper;

    @Autowired
    private StoryService storyService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromptService promptService;

    @Transactional
    public Long submit(Long userId, List<Long> ideaIds, String style, String tone, String length) {
        List<Idea> ideas = ideaMapper.selectList(new LambdaQueryWrapper<Idea>()
                .eq(Idea::getUserId, userId).in(Idea::getId, ideaIds).isNull(Idea::getDeletedAt));
        if (ideas.isEmpty()) throw new IllegalArgumentException("所选想法不存在或无权访问");

        String prompt = promptService.buildPrompt(ideas, style, tone, length);
        String title = promptService.suggestTitle(ideas);
        Story story = storyService.createDraft(userId, title);

        try {
            GenerationTask task = new GenerationTask();
            task.setUserId(userId);
            task.setStoryId(story.getId());
            task.setInputIdeas(objectMapper.writeValueAsString(ideaIds));
            task.setParameters(objectMapper.writeValueAsString(Map.of(
                    "style", style == null ? "" : style,
                    "tone", tone == null ? "" : tone,
                    "length", length == null ? "medium" : length)));
            task.setPromptText(prompt);
            generationTaskMapper.insert(task);

            rabbitTemplate.convertAndSend(QUEUE, task.getId());
            log.info("生成任务已提交: taskId={}, storyId={}, ideaCount={}", task.getId(), story.getId(), ideas.size());
            return task.getId();
        } catch (Exception e) {
            throw new RuntimeException("提交生成任务失败", e);
        }
    }
}
