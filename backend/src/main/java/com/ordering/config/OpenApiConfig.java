package com.ordering.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("在线点餐系统 API")
                        .description("餐厅在线点餐系统 REST API 文档")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("BasicAuth"))
                .schemaRequirement("BasicAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("HTTP Basic 认证（admin/admin123 或 kitchen/kitchen123）"));
    }
}
