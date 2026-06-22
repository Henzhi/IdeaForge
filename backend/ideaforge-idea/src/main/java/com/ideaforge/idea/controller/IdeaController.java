package com.ideaforge.idea.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.PageResponse;
import com.ideaforge.common.api.Result;
import com.ideaforge.idea.dto.*;
import com.ideaforge.idea.service.IdeaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/ideas")
public class IdeaController {

    @Autowired
    private IdeaService ideaService;

    @PostMapping
    public Result<IdeaResp> create(@RequestBody @Valid Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String content = (String) body.get("content");
        Short categoryId = body.get("categoryId") == null ? null : ((Number) body.get("categoryId")).shortValue();
        String clientUuid = (String) body.get("clientUuid");
        return Result.ok(ideaService.create(userId, content, categoryId, clientUuid));
    }

    @PostMapping("/sync")
    public Result<SyncResult> sync(@RequestBody @Valid SyncReq req) {
        return Result.ok(ideaService.sync(StpUtil.getLoginIdAsLong(), req));
    }

    @GetMapping
    public Result<PageResponse<IdeaResp>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Short categoryId,
            @RequestParam(required = false) Boolean archived) {
        return Result.ok(ideaService.list(StpUtil.getLoginIdAsLong(), cursor, limit, categoryId, archived));
    }

    @GetMapping("/search")
    public Result<PageResponse<IdeaResp>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        var items = ideaService.search(StpUtil.getLoginIdAsLong(), q, limit);
        return Result.ok(PageResponse.of(items, null));
    }

    @GetMapping("/semantic-search")
    public Result<PageResponse<IdeaResp>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        var items = ideaService.semanticSearch(StpUtil.getLoginIdAsLong(), query, limit);
        return Result.ok(PageResponse.of(items, null));
    }

    @GetMapping("/debug-embeddings")
    public Result<?> debugEmbeddings() {
        int count = ideaService.debugEmbeddings(StpUtil.getLoginIdAsLong());
        return Result.ok(Map.of("count", count));
    }

    @PostMapping("/generate-embeddings")
    public Result<?> generateEmbeddings() {
        int count = ideaService.generateEmbeddings(StpUtil.getLoginIdAsLong());
        return Result.ok("已生成 " + count + " 条想法的向量");
    }

    @GetMapping("/{id}")
    public Result<IdeaResp> get(@PathVariable Long id) {
        return Result.ok(ideaService.get(StpUtil.getLoginIdAsLong(), id));
    }

    @PutMapping("/{id}")
    public Result<IdeaResp> update(@PathVariable Long id,
                                   @RequestParam(required = false) String content,
                                   @RequestParam(required = false) Short categoryId) {
        return Result.ok(ideaService.update(StpUtil.getLoginIdAsLong(), id, content, categoryId));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        ideaService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/archive")
    public Result<?> archive(@PathVariable Long id, @RequestParam boolean archived) {
        ideaService.toggle(StpUtil.getLoginIdAsLong(), id, "archived", archived);
        return Result.ok();
    }

    @PostMapping("/{id}/pin")
    public Result<?> pin(@PathVariable Long id, @RequestParam boolean pinned) {
        ideaService.toggle(StpUtil.getLoginIdAsLong(), id, "pinned", pinned);
        return Result.ok();
    }
}
