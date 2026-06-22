package com.ideaforge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IdeaForge 启动类。
 * scanBasePackages 扫描所有业务模块的包,使 @ComponentScan 覆盖 com.ideaforge 全域。
 * @MapperScan 扫描所有 Mapper 接口。
 */
@SpringBootApplication(scanBasePackages = "com.ideaforge")
@MapperScan("com.ideaforge.**.mapper")
public class IdeaForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdeaForgeApplication.class, args);
    }
}
