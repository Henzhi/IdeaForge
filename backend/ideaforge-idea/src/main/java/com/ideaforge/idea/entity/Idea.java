package com.ideaforge.idea.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 想法表实体。
 * 软删除 deleted_at + client_uuid 幂等键,数据库层用 partial unique index 保证
 * 软删后同 UUID 可重建(见 Flyway V2 迁移脚本)。
 */
@Entity
@Table(name = "idea", indexes = {
    @Index(name = "idx_idea_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_idea_user_category", columnList = "user_id, category_id")
})
@Data
@NoArgsConstructor
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category_id")
    private Short categoryId;

    /** 是否归档 */
    @Column(name = "is_archived")
    private Boolean archived = false;

    /** 是否置顶 */
    @Column(name = "is_pinned")
    private Boolean pinned = false;

    @Column(name = "client_uuid", length = 36)
    private String clientUuid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
