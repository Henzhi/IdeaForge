package com.ideaforge.story.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 故事表实体。状态: draft / completed / archived。
 * embedding 字段留给向量检索,初期不填充。
 */
@Entity
@Table(name = "story", indexes = {
    @Index(name = "idx_story_user_status", columnList = "user_id, status")
})
@Data
@NoArgsConstructor
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "draft";

    @Column(length = 30)
    private String style;

    @Column(length = 30)
    private String tone;

    @Column(name = "length_preset", length = 20)
    private String lengthPreset;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
