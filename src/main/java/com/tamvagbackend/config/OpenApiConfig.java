package com.tamvagbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tamvaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TAMVA - Financial Identity & Trust Infrastructure API")
                        .description("Engineering API specification for TAMVA real-time transaction trust, risk decisioning, living financial passport, consent state machine, and Dynamic Multi-Currency Wallet.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TAMVA Engineering Team")
                                .email("engineering@tamva.africa"))
                        .license(new License().name("Proprietary - Ghana First / Africa Ready")))
                .servers(List.of(
                        new Server().url("/").description("Current Environment (Render / Local)")
                ));
    }
}
