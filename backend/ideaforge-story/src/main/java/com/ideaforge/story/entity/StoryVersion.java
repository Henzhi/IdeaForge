package com.ideaforge.story.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事版本表实体。
 * 每次生成/重新生成时自动创建一条版本记录,支持回溯任意历史版本。
 */
@Data
@NoArgsConstructor
@TableName("story_version")
public class StoryVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("story_id")
    private Long storyId;

    @TableField("version_number")
    private Integer versionNumber;

    @TableField("content")
    private String content;

    @TableField("word_count")
    private Integer wordCount;

    @TableField("change_summary")
    private String changeSummary;

    @TableField("generation_task_id")
    private Long generationTaskId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
