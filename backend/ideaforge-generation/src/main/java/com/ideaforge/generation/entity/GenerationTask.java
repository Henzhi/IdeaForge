package com.ideaforge.generation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 生成任务表。状态: queued / processing / completed / failed。
 * 异步生成故事的全生命周期追踪。
 */
@Data
@NoArgsConstructor
@TableName("generation_task")
public class GenerationTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("story_id")
    private Long storyId;

    @TableField("model_config_id")
    private Long modelConfigId;

    @TableField("prompt_template_id")
    private Long promptTemplateId;

    @TableField("status")
    private String status = "queued";

    /** 输入想法ID列表及排序,JSON */
    @TableField("input_ideas")
    private String inputIdeas;

    @TableField("parameters")
    private String parameters;

    @TableField("prompt_text")
    private String promptText;

    @TableField("error_message")
    private String errorMessage;

    @TableField("tokens_used")
    private Integer tokensUsed;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
