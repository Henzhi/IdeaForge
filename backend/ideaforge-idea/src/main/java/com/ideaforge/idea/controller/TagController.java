package com.ideaforge.idea.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.Result;
import com.ideaforge.idea.entity.Tag;
import com.ideaforge.idea.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.ok(tagService.list(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Map<String, String> body) {
        return Result.ok(tagService.create(StpUtil.getLoginIdAsLong(),
                body.get("name"), body.get("color")));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(tagService.update(StpUtil.getLoginIdAsLong(), id,
                body.get("name"), body.get("color")));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        tagService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }
}
