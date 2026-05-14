package com.ebook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_TOKEN = "Bearer Token";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("電子書平台 API")
                .description("電子書平台後端 API 文件")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_TOKEN))
            .components(new Components()
                .addSecuritySchemes(BEARER_TOKEN, new SecurityScheme()
                    .name(BEARER_TOKEN)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("UUID")));
    }
}