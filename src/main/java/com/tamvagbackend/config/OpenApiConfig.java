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

                                Use POST /v1/auth/token first to obtain a JWT.
                                In Swagger UI, click Authorize and paste only the
                                access token; Swagger adds the Bearer prefix.

                                Seeded pilot test data:
                                GCB institution = 33333333-3333-3333-3333-333333333333
                                Kwame customer = a1b2c3d4-0000-0000-0000-000000000001
                                Abena customer = a1b2c3d4-0000-0000-0000-000000000002
                                Pilot client ID = app_gcb_pilot_2026
                                Pilot client secret = Configured the client secret through the deployment environment

                                IDs generated at runtime, such as application_id,
                                account_id, connection_id, case_id and passport_id,
                                should be copied from the corresponding GET/POST response.
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
                                                "Paste the access_token returned by " +
                                                "POST /v1/auth/token. Do not include 'Bearer '."
                                        )
                        ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(BEARER_AUTH)
                );
    }
}