package com.ideaforge.idea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideaforge.idea.entity.Idea;

/**
 * 想法 Mapper。复杂查询用 LambdaQueryWrapper 在 Service 层构建。
 */
public interface IdeaMapper extends BaseMapper<Idea> {
}
