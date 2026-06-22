package com.ideaforge.story.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事表实体。状态: draft / completed / archived。
 * embedding 字段留给向量检索,初期不填充。
 */
@Data
@NoArgsConstructor
@TableName("story")
public class Story {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("summary")
    private String summary;

    @TableField("word_count")
    private Integer wordCount = 0;

    @TableField("status")
    private String status = "draft";

    @TableField("style")
    private String style;

    @TableField("tone")
    private String tone;

    @TableField("length_preset")
    private String lengthPreset;

    @TableField("cover_image_url")
    private String coverImageUrl;

    @TableField("view_count")
    private Integer viewCount = 0;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
