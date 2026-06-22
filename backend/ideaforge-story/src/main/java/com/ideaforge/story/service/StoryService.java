package com.ideaforge.story.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.api.PageResponse;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.entity.StoryVersion;
import com.ideaforge.story.mapper.StoryMapper;
import com.ideaforge.story.mapper.StoryVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class StoryService {

    @Autowired
    private StoryMapper storyMapper;

    @Autowired
    private StoryVersionMapper storyVersionMapper;

    // ===== 创建/生成 =====

    @Transactional
    public Story createDraft(Long userId, String title) {
        Story story = new Story();
        story.setUserId(userId);
        story.setTitle(title != null ? title : "未命名故事");
        story.setStatus("draft");
        storyMapper.insert(story);
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

        // 自动创建 v1 版本快照
        saveVersion(story, null, "初始生成");
        log.info("故事生成完成: storyId={}, wordCount={}", storyId, story.getWordCount());
    }

    // ===== 查询 =====

    @Transactional(readOnly = true)
    public Story getOwned(Long userId, Long id) {
        Story story = storyMapper.selectById(id);
        if (story == null || story.getDeletedAt() != null) throw new BizException(ErrorCode.STORY_NOT_FOUND);
        if (!story.getUserId().equals(userId)) throw new BizException(ErrorCode.FORBIDDEN);
        return story;
    }

    @Transactional(readOnly = true)
    public Story getPublicStory(Long id) {
        Story story = storyMapper.selectById(id);
        if (story == null || story.getDeletedAt() != null) throw new BizException(ErrorCode.STORY_NOT_FOUND);
        if (!Boolean.TRUE.equals(story.getIsPublic()) || !"completed".equals(story.getStatus()))
            throw new BizException(ErrorCode.FORBIDDEN);
        story.setViewCount(story.getViewCount() + 1);
        storyMapper.updateById(story);
        return story;
    }

    /** 我的故事(分页) */
    @Transactional(readOnly = true)
    public PageResponse<Story> myStories(Long userId, int page, int size) {
        IPage<Story> result = storyMapper.myStories(Page.of(page, size), userId);
        return PageResponse.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 公开故事广场 */
    @Transactional(readOnly = true)
    public PageResponse<Story> publicStories(int page, int size) {
        IPage<Story> result = storyMapper.publicStories(Page.of(page, size));
        return PageResponse.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ===== 版本 =====

    @Transactional(readOnly = true)
    public List<StoryVersion> versions(Long userId, Long storyId) {
        getOwned(userId, storyId); // 鉴权
        return storyVersionMapper.findVersions(storyId);
    }

    @Transactional
    public Story restore(Long userId, Long storyId, Long versionId) {
        Story story = getOwned(userId, storyId);
        StoryVersion ver = storyVersionMapper.selectById(versionId);
        if (ver == null || !ver.getStoryId().equals(storyId))
            throw new BizException(ErrorCode.STORY_NOT_FOUND);

        // 回溯：先保存当前版本，再替换内容
        saveVersion(story, null, "回溯前快照");
        story.setContent(ver.getContent());
        story.setWordCount(ver.getWordCount());
        story.setStatus("completed");
        storyMapper.updateById(story);

        // 新版本记录
        saveVersion(story, null, "从 v" + ver.getVersionNumber() + " 回溯恢复");
        log.info("故事版本回溯: storyId={}, fromVersion={}", storyId, ver.getVersionNumber());
        return story;
    }

    // ===== 重新生成 =====

    @Transactional
    public Story prepareRegenerate(Long userId, Long storyId) {
        Story story = getOwned(userId, storyId);
        // 保存当前版本
        saveVersion(story, null, "重新生成前快照");
        // 重置为 draft 状态，等待生成任务填充
        story.setContent(null);
        story.setStatus("draft");
        story.setWordCount(0);
        storyMapper.updateById(story);
        return story;
    }

    // ===== 公开/私有 =====

    @Transactional
    public Story publish(Long userId, Long storyId) {
        Story story = getOwned(userId, storyId);
        if (!"completed".equals(story.getStatus()))
            throw new BizException(ErrorCode.STORY_NOT_COMPLETED);
        story.setIsPublic(true);
        storyMapper.updateById(story);
        return story;
    }

    @Transactional
    public Story unpublish(Long userId, Long storyId) {
        Story story = getOwned(userId, storyId);
        story.setIsPublic(false);
        storyMapper.updateById(story);
        return story;
    }

    // ===== 归档/删除 =====

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

    // ===== 内部 =====

    private void saveVersion(Story story, Long taskId, String changeSummary) {
        Integer max = storyVersionMapper.maxVersion(story.getId());
        int nextVer = (max == null ? 0 : max) + 1;
        StoryVersion ver = new StoryVersion();
        ver.setStoryId(story.getId());
        ver.setVersionNumber(nextVer);
        ver.setContent(story.getContent());
        ver.setWordCount(story.getWordCount());
        ver.setChangeSummary(changeSummary);
        ver.setGenerationTaskId(taskId);
        storyVersionMapper.insert(ver);
    }
}
