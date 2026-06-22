package com.ideaforge.story.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.Result;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 故事接口骨架。Sprint 4 补全列表分页、版本管理、发布等接口。
 */
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @GetMapping("/{id}")
    public Result<Story> get(@PathVariable Long id) {
        return Result.ok(storyService.getOwned(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/{id}/archive")
    public Result<?> archive(@PathVariable Long id) {
        storyService.archive(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        storyService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }
}
