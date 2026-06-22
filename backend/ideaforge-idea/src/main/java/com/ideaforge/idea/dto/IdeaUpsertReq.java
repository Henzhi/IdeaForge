package com.ideaforge.idea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IdeaUpsertReq {
    /** 客户端生成的 UUID,同步幂等键 */
    @NotBlank
    @Size(max = 36)
    private String clientUuid;

    @NotBlank
    private String content;

    private Short categoryId;

    /** 客户端最后更新时间,用于 Last-Write-Wins 冲突检测 */
    private LocalDateTime updatedAt;
}
