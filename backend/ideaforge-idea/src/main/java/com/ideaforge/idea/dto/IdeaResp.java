package com.ideaforge.idea.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IdeaResp {
    private Long id;
    private String content;
    private Short categoryId;
    private Boolean archived;
    private Boolean pinned;
    private String clientUuid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
