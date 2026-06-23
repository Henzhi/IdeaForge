package com.ideaforge.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置。
 * <p>
 * <b>Swagger UI 访问</b>：http://localhost:8080/swagger-ui.html
 * <p>
 * <b>Sa-Token 集成</b>：通过 {@code SecurityScheme} 声明请求头 {@code satoken}，
 * 所有接口右上角 "Authorize" 按钮可填入 Token，测试鉴权接口。
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "IdeaForge API",
                version = "0.1.0",
                description = "碎片化想法 → AI 故事生成平台",
                contact = @Contact(name = "IdeaForge Team")
        ),
        security = @SecurityRequirement(name = "Sa-Token")
)
@SecurityScheme(
        name = "Sa-Token",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "satoken"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/api/**")
                .build();
    }
}
