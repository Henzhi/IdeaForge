package com.ideaforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IdeaForge 启动类。
 * scanBasePackages 扫描所有业务模块的包,使 @ComponentScan 覆盖 com.ideaforge 全域。
 */
@SpringBootApplication(scanBasePackages = "com.ideaforge")
public class IdeaForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdeaForgeApplication.class, args);
    }
}
