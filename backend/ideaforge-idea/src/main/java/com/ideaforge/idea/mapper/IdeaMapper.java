package com.ideaforge.idea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideaforge.idea.entity.Idea;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 想法 Mapper。语义搜索用 pgvector,全文搜索用 LambdaQueryWrapper.apply()。
 */
public interface IdeaMapper extends BaseMapper<Idea> {

    /**
     * pgvector 语义搜索:将查询文本输入 AI embedding 服务后得到向量,
     * 与 idea.embedding 计算余弦距离(<=>),取最近 N 条。
     * 调用前需确保:1) idea 的 embedding 已生成;2) queryVector 格式为 '[x1,x2,...]'。
     */
    /**
     * 查出用户所有有 embedding 的想法(含 embedding 文本)。
     * 语义搜索由 Java 层计算余弦相似度(IdeaService)。
     */
    @Select("SELECT id, user_id, content, category_id, is_archived, is_pinned, " +
            "client_uuid, created_at, updated_at, deleted_at, embedding::text AS embedding " +
            "FROM idea WHERE user_id = #{userId} AND deleted_at IS NULL AND embedding IS NOT NULL")
    List<Idea> findAllWithEmbedding(@Param("userId") Long userId);
}
