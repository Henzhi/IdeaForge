package com.ideaforge.idea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ideaforge.idea.entity.Idea;
import com.ideaforge.idea.mapper.IdeaMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Autowired
    private IdeaMapper ideaMapper;

    @Autowired(required = false)
    private OpenAiEmbeddingModel embeddingModel;

    public String embedSingle(String text) {
        checkModel();
        Embedding e = embeddingModel.embed(TextSegment.from(text)).content();
        return formatVector(e.vector());
    }

    @Transactional
    public int generateAll(Long userId) {
        checkModel();
        List<Idea> ideas = ideaMapper.selectList(new LambdaQueryWrapper<Idea>()
                .eq(Idea::getUserId, userId)
                .isNull(Idea::getDeletedAt)
                .apply("embedding IS NULL"));
        if (ideas.isEmpty()) return 0;

        List<TextSegment> segments = ideas.stream()
                .map(i -> TextSegment.from(i.getContent())).toList();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        int count = 0;
        for (int i = 0; i < ideas.size(); i++) {
            Idea idea = ideas.get(i);
            idea.setEmbedding(formatVector(embeddings.get(i).vector()));
            ideaMapper.updateById(idea);
            count++;
        }
        log.info("批量生成 embedding 完成: count={}", count);
        return count;
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.8f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private void checkModel() {
        if (embeddingModel == null)
            throw new IllegalStateException("Embedding 未配置,请设置 app.llm.embedding-base-url");
    }
}
