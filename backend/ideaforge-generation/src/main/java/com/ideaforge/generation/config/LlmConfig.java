package com.ideaforge.generation.config;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 配置。使用 OpenAI 兼容接口,支持 DeepSeek/Qwen/Moonshot/OpenAI 等。
 * 通过 application.yml 的 app.llm.* 配置切换提供商。
 *
 * 条件加载:仅当 app.llm.api-key 非空时创建 Bean,避免无 Key 时启动失败。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.llm.api-key")
public class LlmConfig {

    @Bean
    public OpenAiStreamingChatModel streamingChatModel(
            @Value("${app.llm.api-key}") String apiKey,
            @Value("${app.llm.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.llm.model-name:deepseek-v4-flash}") String modelName,
            @Value("${app.llm.temperature:0.8}") double temperature,
            @Value("${app.llm.max-tokens:3000}") int maxTokens,
            @Value("${app.llm.timeout:60}") long timeoutSeconds) {
        log.info("初始化 LLM: baseUrl={}, model={}, temp={}", baseUrl, modelName, temperature);
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Embedding 模型(用于语义搜索)。
     * OpenAI 兼容接口:硅基流动(BAAI/bge-large-zh-v1.5) / OpenAI(text-embedding-3-small)。
     * Key 优先用 embedding-api-key,未设置时回退到主 api-key。
     * 仅当 app.llm.embedding-base-url 非空时加载。
     */
    @Bean
    @ConditionalOnProperty(name = "app.llm.embedding-base-url")
    public OpenAiEmbeddingModel embeddingModel(
            @Value("${app.llm.api-key}") String apiKey,
            @Value("${app.llm.embedding-api-key:#{null}}") String embeddingApiKey,
            @Value("${app.llm.embedding-base-url}") String embeddingBaseUrl,
            @Value("${app.llm.embedding-model-name:BAAI/bge-large-zh-v1.5}") String embeddingModelName) {
        String key = embeddingApiKey != null && !embeddingApiKey.isBlank() ? embeddingApiKey : apiKey;
        log.info("初始化 Embedding: url={}, model={}", embeddingBaseUrl, embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .apiKey(key)
                .baseUrl(embeddingBaseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}
