package com.ideaforge.idea.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.PageResponse;
import com.ideaforge.common.api.Result;
import com.ideaforge.idea.dto.*;
import com.ideaforge.idea.service.IdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 想法接口。对应 API 文档 /api/v1/ideas。
 */
@Validated
@RestController
@RequestMapping("/api/v1/ideas")
@RequiredArgsConstructor
public class IdeaController {

    private final IdeaService ideaService;

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
