package com.ideaforge.generation.config;

import com.ideaforge.generation.service.GenerationService;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列声明。
 * Spring 启动时由 RabbitAdmin 自动创建队列(若不存在)。
 * 消费者的 @RabbitListener 依赖此声明,否则新 RabbitMQ 实例会因队列不存在而启动失败。
 */
@Configuration
public class RabbitMQConfig {

    /** 故事生成任务队列(持久化,防止重启丢消息) */
    @Bean
    public Queue storyGenerationQueue() {
        return QueueBuilder.durable(GenerationService.QUEUE)
                .build();
    }
}
