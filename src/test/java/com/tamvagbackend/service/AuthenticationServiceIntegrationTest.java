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
        System.getenv("TAMVA_DB_URL");

    private static final String TEST_JWT_SECRET =
        System.getenv("TAMVA_TEST_JWT_SECRET");

    private static final String SEED_CLIENT_SECRET =
        System.getenv("TAMVA_SEED_CLIENT_SECRET");

    private static final String S_USERNAME =
        System.getenv("SPRING_DATASOURCE_USERNAME");

    private static final String S_PASSWORD =
        System.getenv("SPRING_DATASOURCE_PASSWORD");

    private static final String S_DRIVER_CLASS =
        System.getenv("SPRING_DATASOURCE_DRIVER_CLASS_NAME");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DB_URL);
        registry.add("spring.datasource.username", () -> S_USERNAME);
        registry.add("spring.datasource.password", () -> S_PASSWORD);
        registry.add("spring.datasource.driver-class-name",
                () -> S_DRIVER_CLASS);

        registry.add("spring.flyway.url", () -> DB_URL);
        registry.add("spring.flyway.user", () -> S_USERNAME);
        registry.add("spring.flyway.password", () -> S_PASSWORD);

        registry.add(
                "tamva.security.jwt-secret",
                () -> TEST_JWT_SECRET
        );
    }

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    void realDatabaseAuthenticationReturnsAccessToken() {

        assertNotNull(DB_URL,
                "TAMVA_DB_URL must be set for this test");
        assertNotNull(S_USERNAME,
                "SPRING_DATASOURCE_USERNAME must be set for this test");
        assertNotNull(S_PASSWORD,
                "SPRING_DATASOURCE_PASSWORD must be set for this test");
        assertNotNull(S_DRIVER_CLASS,
                "SPRING_DATASOURCE_DRIVER_CLASS_NAME must be set for this test");
        assertNotNull(TEST_JWT_SECRET,
                "TAMVA_TEST_JWT_SECRET must be set for this test");
        assertNotNull(SEED_CLIENT_SECRET,
                "TAMVA_SEED_CLIENT_SECRET must be set for this test");

        TokenResponse response = authenticationService.authenticate(
                new TokenRequest(
                        "app_gcb_pilot_2026",
                        SEED_CLIENT_SECRET
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