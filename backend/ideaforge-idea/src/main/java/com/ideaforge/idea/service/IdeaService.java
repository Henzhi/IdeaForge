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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class IdeaService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private IdeaMapper ideaMapper;

    @Autowired
    private EmbeddingService embeddingService;

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
                if (idea.getId() != null && idea.getUpdatedAt() != null
                        && item.getUpdatedAt() != null
                        && item.getUpdatedAt().isBefore(idea.getUpdatedAt())) {
                    conflicts.add(item.getClientUuid());
                    continue;
                }
                idea.setContent(item.getContent());
                idea.setCategoryId(item.getCategoryId());
                if (item.getUpdatedAt() != null) idea.setUpdatedAt(item.getUpdatedAt());
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

    @Transactional(readOnly = true)
    public IdeaResp get(Long userId, Long id) {
        return toResp(getOwned(userId, id));
    }

    @Transactional
    public IdeaResp update(Long userId, Long id, String content, Short categoryId) {
        Idea idea = getOwned(userId, id);
        if (content != null) idea.setContent(content);
        if (categoryId != null) idea.setCategoryId(categoryId);
        ideaMapper.updateById(idea);
        return toResp(idea);
    }

    @Transactional
    public void toggle(Long userId, Long id, String field, boolean value) {
        Idea idea = getOwned(userId, id);
        if ("archived".equals(field)) idea.setArchived(value);
        else if ("pinned".equals(field)) idea.setPinned(value);
        ideaMapper.updateById(idea);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Idea idea = getOwned(userId, id);
        idea.setDeletedAt(LocalDateTime.now());
        ideaMapper.updateById(idea);
    }

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

    @Transactional(readOnly = true)
    public List<IdeaResp> semanticSearch(Long userId, String query, int limit) {
        String queryVectorStr = embeddingService.embedSingle(query);
        float[] queryVec = parseVector(queryVectorStr);
        List<Idea> ideas = ideaMapper.findAllWithEmbedding(userId);
        log.info("语义搜索: query={}, embeddingCount={}, queryVecDim={}", query, ideas.size(), queryVec.length);

        return ideas.stream()
                .map(i -> {
                    String embStr = i.getEmbedding();
                    if (embStr == null || embStr.length() < 10) {
                        log.warn("  idea id={} embedding 为空/过短", i.getId());
                        return new IdeaScore(i, Double.NEGATIVE_INFINITY);
                    }
                    float[] emb = parseVector(embStr);
                    double score = cosineSimilarity(emb, queryVec);
                    return new IdeaScore(i, score);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .peek(s -> log.info("  idea id={} score={}", s.idea.getId(), String.format("%.6f", s.score)))
                .map(s -> toResp(s.idea))
                .toList();
    }

    @Transactional
    public int generateEmbeddings(Long userId) {
        return embeddingService.generateAll(userId);
    }

    @Transactional(readOnly = true)
    public int debugEmbeddings(Long userId) {
        return ideaMapper.findAllWithEmbedding(userId).size();
    }

    private record IdeaScore(Idea idea, double score) {}

    private float[] parseVector(String vec) {
        String[] parts = vec.substring(1, vec.length() - 1).split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Float.parseFloat(parts[i]);
        return arr;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return normA == 0 || normB == 0 ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Idea getOwned(Long userId, Long id) {
        Idea idea = ideaMapper.selectById(id);
        if (idea == null) throw new BizException(ErrorCode.IDEA_NOT_FOUND);
        if (!idea.getUserId().equals(userId) || idea.getDeletedAt() != null)
            throw new BizException(ErrorCode.IDEA_PERMISSION_DENIED);
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
