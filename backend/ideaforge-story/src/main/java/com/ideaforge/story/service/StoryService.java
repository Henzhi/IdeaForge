package com.ideaforge.story.service;

import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 故事服务。Sprint 4 完善:列表分页、版本管理、发布/归档。
 * 当前提供基础 CRUD 与状态更新,生成流程由 ideaforge-generation 模块触发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;

    @Transactional
    public Story createDraft(Long userId, String title) {
        Story story = new Story();
        story.setUserId(userId);
        story.setTitle(title != null ? title : "未命名故事");
        story.setStatus("draft");
        return storyRepository.save(story);
    }

    @Transactional(readOnly = true)
    public Story getOwned(Long userId, Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.STORY_NOT_FOUND));
        if (!story.getUserId().equals(userId) || story.getDeletedAt() != null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return story;
    }

    /** 生成完成后由 generation 模块调用,写入正文并置为 completed */
    @Transactional
    public void markCompleted(Long storyId, String content, String summary) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BizException(ErrorCode.STORY_NOT_FOUND));
        story.setContent(content);
        story.setSummary(summary);
        story.setWordCount(content == null ? 0 : content.length());
        story.setStatus("completed");
        storyRepository.save(story);
        log.info("故事生成完成: storyId={}, wordCount={}", storyId, story.getWordCount());
    }

    @Transactional
    public void archive(Long userId, Long id) {
        Story story = getOwned(userId, id);
        story.setStatus("archived");
        storyRepository.save(story);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Story story = getOwned(userId, id);
        story.setDeletedAt(java.time.LocalDateTime.now());
        storyRepository.save(story);
    }
}
