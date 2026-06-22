package com.ideaforge.story.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.PageResponse;
import com.ideaforge.common.api.Result;
import com.ideaforge.story.entity.Story;
import com.ideaforge.story.entity.StoryVersion;
import com.ideaforge.story.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {

    @Autowired
    private StoryService storyService;

    // ===== 查询 =====

    /** 我的故事列表(分页) */
    @GetMapping("/my")
    public Result<PageResponse<Story>> myStories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(storyService.myStories(StpUtil.getLoginIdAsLong(), page, size));
    }

    /** 公开故事广场 */
    @GetMapping("/public")
    public Result<PageResponse<Story>> publicStories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(storyService.publicStories(page, size));
    }

    /** 故事详情 */
    @GetMapping("/{id}")
    public Result<Story> get(@PathVariable Long id) {
        return Result.ok(storyService.getOwned(StpUtil.getLoginIdAsLong(), id));
    }

    /** 公开故事详情(无需登录) */
    @GetMapping("/public/{id}")
    public Result<Story> getPublic(@PathVariable Long id) {
        return Result.ok(storyService.getPublicStory(id));
    }

    // ===== 版本 =====

    /** 版本历史 */
    @GetMapping("/{id}/versions")
    public Result<java.util.List<StoryVersion>> versions(@PathVariable Long id) {
        return Result.ok(storyService.versions(StpUtil.getLoginIdAsLong(), id));
    }

    /** 回溯到指定版本 */
    @PostMapping("/{id}/versions/{versionId}/restore")
    public Result<Story> restore(@PathVariable Long id, @PathVariable Long versionId) {
        return Result.ok(storyService.restore(StpUtil.getLoginIdAsLong(), id, versionId));
    }

    // ===== 重新生成 =====

    /** 重新生成(重置为 draft,需重新调 /generate 提交) */
    @PostMapping("/{id}/regenerate")
    public Result<Story> regenerate(@PathVariable Long id) {
        return Result.ok(storyService.prepareRegenerate(StpUtil.getLoginIdAsLong(), id));
    }

    // ===== 公开/私有 =====

    @PutMapping("/{id}/publish")
    public Result<Story> publish(@PathVariable Long id) {
        return Result.ok(storyService.publish(StpUtil.getLoginIdAsLong(), id));
    }

    @PutMapping("/{id}/unpublish")
    public Result<Story> unpublish(@PathVariable Long id) {
        return Result.ok(storyService.unpublish(StpUtil.getLoginIdAsLong(), id));
    }

    // ===== 归档/删除 =====

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
