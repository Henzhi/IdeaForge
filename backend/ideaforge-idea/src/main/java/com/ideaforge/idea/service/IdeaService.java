package com.ideaforge.idea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.api.PageResponse;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.idea.dto.*;
import com.ideaforge.idea.entity.Idea;
import com.ideaforge.idea.mapper.IdeaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 想法服务。含 CRUD、批量同步(幂等 + Last-Write-Wins 冲突处理)。
 * 同步逻辑遵循 补充.md 第 5.3 节:clientUuid 为幂等键,updatedAt 为冲突判定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    private final IdeaMapper ideaMapper;

    /** 创建单条想法 */
    @Transactional
    public IdeaResp create(Long userId, String content, Short categoryId, String clientUuid) {
        Idea idea = new Idea();
        idea.setUserId(userId);
        idea.setContent(content);
        idea.setCategoryId(categoryId);
        idea.setClientUuid(clientUuid);
        ideaMapper.insert(idea);
        return toResp(idea);
    }

    /** 游标分页查询 */
    @Transactional(readOnly = true)
    public PageResponse<IdeaResp> list(Long userId, String cursor, int limit, Short categoryId, Boolean archived) {
        LocalDateTime cursorTime = parseCursor(cursor);
        LambdaQueryWrapper<Idea> wrapper = new LambdaQueryWrapper<Idea>()
                .eq(Idea::getUserId, userId)
                .isNull(Idea::getDeletedAt)
                .lt(cursorTime != null, Idea::getCreatedAt, cursorTime)
                .eq(categoryId != null, Idea::getCategoryId, categoryId)
                .eq(archived != null, Idea::getArchived, archived)
                .orderByDesc(Idea::getPinned)
                .orderByDesc(Idea::getCreatedAt);
        Page<Idea> page = ideaMapper.selectPage(new Page<>(1, limit + 1), wrapper);
        List<Idea> content = page.getRecords();
        boolean hasMore = content.size() > limit;
        List<Idea> items = hasMore ? content.subList(0, limit) : content;
        String nextCursor = hasMore ? items.get(items.size() - 1).getCreatedAt().format(ISO) : null;
        return new PageResponse<>(items.stream().map(this::toResp).toList(), nextCursor, hasMore);
    }

    /** 批量同步(核心) */
    @Transactional
    public SyncResult sync(Long userId, SyncReq req) {
        List<IdeaResp> synced = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        if (req.getUpserts() != null) {
            for (IdeaUpsertReq item : req.getUpserts()) {
                Idea idea = ideaMapper.selectOne(new LambdaQueryWrapper<Idea>()
                        .eq(Idea::getUserId, userId)
                        .eq(Idea::getClientUuid, item.getClientUuid())
                        .isNull(Idea::getDeletedAt));
                if (idea == null) {
                    idea = new Idea();
                    idea.setUserId(userId);
                    idea.setClientUuid(item.getClientUuid());
                }

                // Last-Write-Wins: 服务端时间更新者优先
                if (idea.getId() != null && idea.getUpdatedAt() != null
                        && item.getUpdatedAt() != null
                        && item.getUpdatedAt().isBefore(idea.getUpdatedAt())) {
                    conflicts.add(item.getClientUuid());
                    continue;
                }

                idea.setContent(item.getContent());
                idea.setCategoryId(item.getCategoryId());
                if (item.getUpdatedAt() != null) {
                    idea.setUpdatedAt(item.getUpdatedAt());
                }
                if (idea.getId() == null) {
                    ideaMapper.insert(idea);
                } else {
                    ideaMapper.updateById(idea);
                }
                synced.add(toResp(idea));
            }
        }

        if (req.getDeletes() != null) {
            for (String clientUuid : req.getDeletes()) {
                ideaMapper.update(null, new LambdaUpdateWrapper<Idea>()
                        .eq(Idea::getUserId, userId)
                        .eq(Idea::getClientUuid, clientUuid)
                        .isNull(Idea::getDeletedAt)
                        .set(Idea::getDeletedAt, LocalDateTime.now()));
            }
        }
        return new SyncResult(synced, conflicts);
    }

    /** 获取详情 */
    @Transactional(readOnly = true)
    public IdeaResp get(Long userId, Long id) {
        return toResp(getOwned(userId, id));
    }

    /** 更新 */
    @Transactional
    public IdeaResp update(Long userId, Long id, String content, Short categoryId) {
        Idea idea = getOwned(userId, id);
        if (content != null) idea.setContent(content);
        if (categoryId != null) idea.setCategoryId(categoryId);
        ideaMapper.updateById(idea);
        return toResp(idea);
    }

    /** 归档/置顶切换 */
    @Transactional
    public void toggle(Long userId, Long id, String field, boolean value) {
        Idea idea = getOwned(userId, id);
        if ("archived".equals(field)) idea.setArchived(value);
        else if ("pinned".equals(field)) idea.setPinned(value);
        ideaMapper.updateById(idea);
    }

    /** 软删除 */
    @Transactional
    public void delete(Long userId, Long id) {
        Idea idea = getOwned(userId, id);
        idea.setDeletedAt(LocalDateTime.now());
        ideaMapper.updateById(idea);
    }

    // ===== 搜索 =====

    /** 全文搜索(PostgreSQL tsvector)。使用中文分词器 plainto_tsquery。 */
    @Transactional(readOnly = true)
    public List<IdeaResp> search(Long userId, String keyword, int limit) {
        List<Idea> ideas = ideaMapper.selectList(new LambdaQueryWrapper<Idea>()
                .eq(Idea::getUserId, userId)
                .isNull(Idea::getDeletedAt)
                .apply("to_tsvector('simple', content) @@ plainto_tsquery('simple', {0})", keyword)
                .orderByDesc(Idea::getCreatedAt)
                .last("LIMIT " + limit));
        return ideas.stream().map(this::toResp).toList();
    }

    /**
     * 语义搜索(pgvector 余弦距离)。
     * 前置条件:embedding 字段已由 AI 服务生成(调用 /ideas/{id}/embed 或批量生成)。
     */
    @Transactional(readOnly = true)
    public List<IdeaResp> semanticSearch(Long userId, String query, int limit) {
        // 调用 Mapper 中自定义 SQL,用 pgvector <=> 余弦距离排序
        List<Idea> ideas = ideaMapper.semanticSearch(userId, query, limit);
        return ideas.stream().map(this::toResp).toList();
    }

    // ===== 内部方法 =====

    private Idea getOwned(Long userId, Long id) {
        Idea idea = ideaMapper.selectById(id);
        if (idea == null) {
            throw new BizException(ErrorCode.IDEA_NOT_FOUND);
        }
        if (!idea.getUserId().equals(userId) || idea.getDeletedAt() != null) {
            throw new BizException(ErrorCode.IDEA_PERMISSION_DENIED);
        }
        return idea;
    }

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        return LocalDateTime.parse(cursor, ISO);
    }

    private IdeaResp toResp(Idea idea) {
        IdeaResp resp = new IdeaResp();
        resp.setId(idea.getId());
        resp.setContent(idea.getContent());
        resp.setCategoryId(idea.getCategoryId());
        resp.setArchived(idea.getArchived());
        resp.setPinned(idea.getPinned());
        resp.setClientUuid(idea.getClientUuid());
        resp.setCreatedAt(idea.getCreatedAt());
        resp.setUpdatedAt(idea.getUpdatedAt());
        return resp;
    }
}
