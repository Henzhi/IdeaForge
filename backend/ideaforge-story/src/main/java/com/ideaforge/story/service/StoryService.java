package com.ideaforge.story.service;

import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.mapper.StoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class StoryService {

    @Autowired
    private StoryMapper storyMapper;

    @Transactional
    public Story createDraft(Long userId, String title) {
        Story story = new Story();
        story.setUserId(userId);
        story.setTitle(title != null ? title : "未命名故事");
        story.setStatus("draft");
        storyMapper.insert(story);
        return story;
    }

    @Transactional(readOnly = true)
    public Story getOwned(Long userId, Long id) {
        Story story = storyMapper.selectById(id);
        if (story == null) throw new BizException(ErrorCode.STORY_NOT_FOUND);
        if (!story.getUserId().equals(userId) || story.getDeletedAt() != null)
            throw new BizException(ErrorCode.FORBIDDEN);
        return story;
    }

    @Transactional
    public void markCompleted(Long storyId, String content, String summary) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) throw new BizException(ErrorCode.STORY_NOT_FOUND);
        story.setContent(content);
        story.setSummary(summary);
        story.setWordCount(content == null ? 0 : content.length());
        story.setStatus("completed");
        storyMapper.updateById(story);
        log.info("故事生成完成: storyId={}, wordCount={}", storyId, story.getWordCount());
    }

    @Transactional
    public void archive(Long userId, Long id) {
        Story story = getOwned(userId, id);
        story.setStatus("archived");
        storyMapper.updateById(story);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Story story = getOwned(userId, id);
        story.setDeletedAt(LocalDateTime.now());
        storyMapper.updateById(story);
    }
}
