package com.ideaforge.idea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.idea.entity.IdeaTag;
import com.ideaforge.idea.entity.Tag;
import com.ideaforge.idea.mapper.IdeaTagMapper;
import com.ideaforge.idea.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final IdeaTagMapper ideaTagMapper;

    /** 获取用户所有标签 */
    public List<Tag> list(Long userId) {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId).orderByAsc(Tag::getCreatedAt));
    }

    /** 获取某个想法已绑定的标签ID列表 */
    public List<Long> getTagIdsByIdeaId(Long ideaId) {
        return ideaTagMapper.selectList(new LambdaQueryWrapper<IdeaTag>()
                .eq(IdeaTag::getIdeaId, ideaId))
                .stream().map(IdeaTag::getTagId).toList();
    }

    /** 创建标签(同用户下不重名) */
    @Transactional
    public Tag create(Long userId, String name, String color) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId).eq(Tag::getName, name));
        if (count > 0) throw new BizException(ErrorCode.CONFLICT.getCode(), "标签名已存在");
        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(name);
        tag.setColor(color == null ? "#AAAAAA" : color);
        tagMapper.insert(tag);
        return tag;
    }

    /** 更新标签 */
    @Transactional
    public Tag update(Long userId, Long id, String name, String color) {
        Tag tag = getOwned(userId, id);
        if (name != null) tag.setName(name);
        if (color != null) tag.setColor(color);
        tagMapper.updateById(tag);
        return tag;
    }

    /** 删除标签(并清除关联) */
    @Transactional
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        ideaTagMapper.delete(new LambdaQueryWrapper<IdeaTag>().eq(IdeaTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    private Tag getOwned(Long userId, Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || !tag.getUserId().equals(userId))
            throw new BizException(ErrorCode.NOT_FOUND);
        return tag;
    }
}
