package com.ideaforge.story.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideaforge.story.entity.Story;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StoryMapper extends BaseMapper<Story> {

    /** 我的故事分页(排除已删) */
    @Select("SELECT * FROM story WHERE user_id = #{userId} AND deleted_at IS NULL ORDER BY created_at DESC")
    IPage<Story> myStories(Page<Story> page, @Param("userId") Long userId);

    /** 公开故事广场(仅已完成+公开+未删) */
    @Select("SELECT * FROM story WHERE is_public = TRUE AND status = 'completed' AND deleted_at IS NULL ORDER BY view_count DESC, created_at DESC")
    IPage<Story> publicStories(Page<Story> page);
}
