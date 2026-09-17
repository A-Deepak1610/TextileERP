package com.textile.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI textileErpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TexForge ERP API")
                        .description("Multi-tenant Enterprise Resource Planning (ERP) REST API for textile manufacturing, yarn spinning, weaving, inventory, order processing, and user management.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("TexForge Engineering Team")
                                .email("dev@texforge.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://texforge.com/terms")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token in the format: Bearer <token>")));
    }
}
