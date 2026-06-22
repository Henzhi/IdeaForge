package com.ideaforge.idea.service;

import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.api.PageResponse;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.idea.dto.*;
import com.ideaforge.idea.entity.Idea;
import com.ideaforge.idea.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final IdeaRepository ideaRepository;

    /** 创建单条想法 */
    @Transactional
    public IdeaResp create(Long userId, String content, Short categoryId, String clientUuid) {
        Idea idea = new Idea();
        idea.setUserId(userId);
        idea.setContent(content);
        idea.setCategoryId(categoryId);
        idea.setClientUuid(clientUuid);
        return toResp(ideaRepository.save(idea));
    }

    /** 游标分页查询 */
    @Transactional(readOnly = true)
    public PageResponse<IdeaResp> list(Long userId, String cursor, int limit, Short categoryId, Boolean archived) {
        LocalDateTime cursorTime = parseCursor(cursor);
        Page<Idea> page = ideaRepository.findByCursor(userId, cursorTime, categoryId, archived,
                PageRequest.of(0, limit + 1));
        List<Idea> content = page.getContent();
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
                Idea idea = ideaRepository
                        .findByUserIdAndClientUuidAndDeletedAtIsNull(userId, item.getClientUuid())
                        .orElseGet(() -> {
                            Idea fresh = new Idea();
                            fresh.setUserId(userId);
                            fresh.setClientUuid(item.getClientUuid());
                            return fresh;
                        });

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
                synced.add(toResp(ideaRepository.save(idea)));
            }
        }

        if (req.getDeletes() != null) {
            for (String clientUuid : req.getDeletes()) {
                ideaRepository.softDeleteByClientUuid(userId, clientUuid);
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
        return toResp(ideaRepository.save(idea));
    }

    /** 归档/置顶切换 */
    @Transactional
    public void toggle(Long userId, Long id, String field, boolean value) {
        Idea idea = getOwned(userId, id);
        if ("archived".equals(field)) idea.setArchived(value);
        else if ("pinned".equals(field)) idea.setPinned(value);
        ideaRepository.save(idea);
    }

    /** 软删除 */
    @Transactional
    public void delete(Long userId, Long id) {
        Idea idea = getOwned(userId, id);
        idea.setDeletedAt(LocalDateTime.now());
        ideaRepository.save(idea);
    }

    // ===== 内部方法 =====

    private Idea getOwned(Long userId, Long id) {
        Idea idea = ideaRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.IDEA_NOT_FOUND));
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
