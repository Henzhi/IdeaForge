package com.ideaforge.generation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.Result;
import com.ideaforge.generation.service.GenerationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stories")
public class GenerationController {

    @Autowired
    private GenerationService generationService;

    @PostMapping("/generate")
    public Result<Map<String, Long>> generate(@RequestBody GenerateReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
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
