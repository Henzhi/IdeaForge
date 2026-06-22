package com.ideaforge.generation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 生成任务表。状态: queued / processing / completed / failed。
 * 异步生成故事的全生命周期追踪。
 */
@Entity
@Table(name = "generation_task", indexes = {
    @Index(name = "idx_task_user_status", columnList = "user_id, status"),
    @Index(name = "idx_task_status_created", columnList = "status, created_at")
})
@Data
@NoArgsConstructor
public class GenerationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "story_id")
    private Long storyId;

    @Column(name = "model_config_id")
    private Long modelConfigId;

    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    @Column(nullable = false, length = 20)
    private String status = "queued";

    /** 输入想法ID列表及排序,JSON */
    @Column(name = "input_ideas", nullable = false, columnDefinition = "JSONB")
    private String inputIdeas;

    @Column(columnDefinition = "JSONB")
    private String parameters;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
