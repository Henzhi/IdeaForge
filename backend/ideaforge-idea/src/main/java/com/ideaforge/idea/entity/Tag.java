package com.ideaforge.idea.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签表。同用户下标签名唯一(uk_tag_name_user)。
 */
@Data
@NoArgsConstructor
@TableName("tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    /** 标签颜色(hex) */
    @TableField("color")
    private String color = "#AAAAAA";

    @TableField("user_id")
    private Long userId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
