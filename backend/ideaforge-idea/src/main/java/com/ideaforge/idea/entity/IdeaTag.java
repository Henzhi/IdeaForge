package com.ideaforge.idea.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 想法-标签关联(多对多中间表)。
 * 纯粹关系表,无单独 ID,双字段联合主键由 DB 约束管理。
 */
@Data
@TableName("idea_tag")
public class IdeaTag {
    @TableField("idea_id")
    private Long ideaId;

    @TableField("tag_id")
    private Long tagId;
}
