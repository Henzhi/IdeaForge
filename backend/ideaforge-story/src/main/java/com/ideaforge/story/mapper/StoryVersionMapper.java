package com.ideaforge.story.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideaforge.story.entity.StoryVersion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StoryVersionMapper extends BaseMapper<StoryVersion> {

    @Select("SELECT * FROM story_version WHERE story_id = #{storyId} ORDER BY version_number DESC")
    List<StoryVersion> findVersions(@Param("storyId") Long storyId);

    @Select("SELECT MAX(version_number) FROM story_version WHERE story_id = #{storyId}")
    Integer maxVersion(@Param("storyId") Long storyId);
}
