package com.ideaforge.generation.service;

import com.ideaforge.idea.entity.Idea;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词组装服务。
 * 将用户选中的想法 + 生成参数(风格/语气/长度)组装为发给 LLM 的完整提示词。
 * 模板可通过 application.yml 的 app.llm.prompt-template 自定义。
 */
@Service
public class PromptService {

    @Value("${app.llm.prompt-template:你是一个创意作家，根据以下思想碎片创作一个连贯的故事。\n\n风格：{style}\n视角：{tone}\n长度：{length}\n\n思想碎片：\n{ideas}}")
    private String promptTemplate;

    /** 组装完整提示词 */
    public String buildPrompt(List<Idea> ideas, String style, String tone, String length) {
        String ideasText = ideas.stream()
                .map(i -> "- " + i.getContent())
                .collect(Collectors.joining("\n"));
        return promptTemplate
                .replace("{style}", style == null || style.isBlank() ? "自由发挥" : style)
                .replace("{tone}", tone == null || tone.isBlank() ? "第三人称" : tone)
                .replace("{length}", length == null || length.isBlank() ? "中等篇幅" : length)
                .replace("{ideas}", ideasText);
    }

    /** 根据想法内容生成故事标题建议(取前 2 条想法的关键词) */
    public String suggestTitle(List<Idea> ideas) {
        if (ideas == null || ideas.isEmpty()) return "未命名故事";
        Idea first = ideas.get(0);
        String content = first.getContent();
        if (content.length() <= 15) return content;
        return content.substring(0, 15) + "...";
    }
}
