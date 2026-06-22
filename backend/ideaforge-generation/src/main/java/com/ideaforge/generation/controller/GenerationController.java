package com.ideaforge.generation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.Result;
import com.ideaforge.generation.service.GenerationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 故事生成触发接口。对应 /api/v1/stories/generate。
 * 校验想法归属后提交异步任务。
 */
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationService generationService;

    @PostMapping("/generate")
    public Result<Map<String, Long>> generate(@RequestBody GenerateReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        // TODO: 校验 ideaIds 是否属于当前用户
        Long taskId = generationService.submit(userId, req.getIdeaIds(),
                req.getStyle(), req.getTone(), req.getLength());
        return Result.ok(Map.of("taskId", taskId));
    }

    @Data
    public static class GenerateReq {
        private List<Long> ideaIds;
        private String style;
        private String tone;
        private String length;
    }
}
