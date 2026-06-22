package com.ideaforge.idea.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ideaforge.idea.config.VectorTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 想法表实体。
 * 软删除 deleted_at + client_uuid 幂等键,数据库层用 partial unique index 保证
 * 软删后同 UUID 可重建(见 Flyway V2 迁移脚本)。
 */
@Data
@NoArgsConstructor
@TableName("idea")
public class Idea {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("content")
    private String content;

    @TableField("category_id")
    private Short categoryId;

    /** 是否归档 */
    @TableField("is_archived")
    private Boolean archived = false;

    /** 是否置顶 */
    @TableField("is_pinned")
    private Boolean pinned = false;

    @TableField("client_uuid")
    private String clientUuid;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /** pgvector 嵌入向量(语义搜索) */
    @TableField(value = "embedding", typeHandler = VectorTypeHandler.class)
    private String embedding;
}
