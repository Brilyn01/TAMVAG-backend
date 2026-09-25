package com.tamvagbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
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

                                Use POST /v1/auth/token or POST /v1/users/signin first to obtain a JWT.
                                In Swagger UI, click Authorize and paste only the
                                access token; Swagger adds the Bearer prefix.
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
                                .description("Current Environment (Local / Staging / Production)")
                ))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Paste the access_token returned by " +
                                                "POST /v1/auth/token or /v1/users/signin. Do not include 'Bearer '."
                                        )
                        ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(BEARER_AUTH)
                );
    }

    @Bean
    public GroupedOpenApi allApiGroup() {
        return GroupedOpenApi.builder()
                .group("0-All-APIs")
                .pathsToMatch("/v1/**", "/actuator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApiGroup() {
        return GroupedOpenApi.builder()
                .group("1-Auth-APIs")
                .pathsToMatch("/v1/auth/**", "/v1/admin/auth/**", "/v1/users/signup", "/v1/users/signin", "/v1/users/refresh", "/v1/users/logout")
                .build();
    }

    @Bean
    public GroupedOpenApi mobileApiGroup() {
        return GroupedOpenApi.builder()
                .group("2-Mobile-APIs")
                .pathsToMatch("/v1/users/**", "/v1/wallets/**", "/v1/transfers/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("3-Admin-APIs")
                .pathsToMatch("/v1/admin/**", "/v1/cases/**")
                .build();
    }

    @Bean
    public GroupedOpenApi webPartnerApiGroup() {
        return GroupedOpenApi.builder()
                .group("4-Web-Partner-APIs")
                .pathsToMatch("/v1/risk/**", "/v1/consents/**", "/v1/connections/**", "/v1/institutions/**", "/v1/health")
                .build();
    }
}