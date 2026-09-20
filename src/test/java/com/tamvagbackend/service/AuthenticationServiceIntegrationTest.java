package com.tamvagbackend.service;

import com.tamvagbackend.dto.AuthDtos.TokenRequest;
import com.tamvagbackend.dto.AuthDtos.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationServiceIntegrationTest {

    private static final String DB_URL =
            "jdbc:postgresql://localhost:5432/tamva";

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DB_URL);
        registry.add("spring.datasource.username", () -> "tamva_user");
        registry.add("spring.datasource.password", () -> "tamva_pass");
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        registry.add("spring.flyway.url", () -> DB_URL);
        registry.add("spring.flyway.user", () -> "tamva_user");
        registry.add("spring.flyway.password", () -> "tamva_pass");
        registry.add(
        "tamva.security.jwt-secret",
            () -> "test-jwt-secret-2026-for-local-integration-testing-32chars!"
        );

    }

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    void realDatabaseAuthenticationReturnsAccessToken() {

        TokenResponse response = authenticationService.authenticate(
                new TokenRequest(
                        "app_gcb_pilot_2026",
                        "gcb-pilot-secret-2026"
                )
        );

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertFalse(response.accessToken().isBlank());

        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());

        assertEquals(
                "risk:evaluate profile:read consent:create connector:sync connector:read",
                response.scope()
        );
    }
}
