package com.ideaforge.story.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.Result;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {

    @Autowired
    private StoryService storyService;

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
