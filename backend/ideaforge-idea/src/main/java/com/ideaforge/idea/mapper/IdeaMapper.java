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
    @Select("SELECT * FROM idea WHERE user_id = #{userId} AND deleted_at IS NULL AND embedding IS NOT NULL ORDER BY embedding <=> #{queryVector}::vector LIMIT #{limit}")
    List<Idea> semanticSearch(@Param("userId") Long userId,
                              @Param("queryVector") String queryVector,
                              @Param("limit") int limit);
}
