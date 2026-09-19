package com.tamvagbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI tamvaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TAMVA - Financial Identity & Trust Infrastructure API")
                        .description("""
                                Engineering API specification for TAMVA.

                                This API provides authentication, application management,
                                customer profiles, consent management, risk decisioning,
                                transaction ingestion, cases, connector synchronization,
                                webhooks, financial passport, audit, and multi-currency
                                wallet capabilities.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TAMVA Engineering Team")
                                .email("engineering@tamva.africa"))
                        .license(new License()
                                .name("Proprietary - Ghana First / Africa Ready")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Current Environment (Render / Local)")
                ))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Enter the JWT access token returned by " +
                                                "POST /v1/auth/token. Do not include the " +
                                                "'Bearer ' prefix."
                                        )
                        ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(BEARER_AUTH)
                );
    }
}