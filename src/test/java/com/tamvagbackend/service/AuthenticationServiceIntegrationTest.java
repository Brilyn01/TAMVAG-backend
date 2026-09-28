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
        System.getenv("TAMVA_DB_URL") != null && !System.getenv("TAMVA_DB_URL").isBlank()
            ? System.getenv("TAMVA_DB_URL")
            : "jdbc:postgresql://localhost:5432/tamva";

    private static final String TEST_JWT_SECRET =
        System.getenv("TAMVA_TEST_JWT_SECRET") != null && !System.getenv("TAMVA_TEST_JWT_SECRET").isBlank()
            ? System.getenv("TAMVA_TEST_JWT_SECRET")
            : "fa07f7202f41b534f8aff5ddd3650ffcb5a3cdf5385e03a43ab931096eeb5580";

    private static final String SEED_CLIENT_SECRET =
        System.getenv("TAMVA_SEED_CLIENT_SECRET") != null && !System.getenv("TAMVA_SEED_CLIENT_SECRET").isBlank()
            ? System.getenv("TAMVA_SEED_CLIENT_SECRET")
            : "gcb-pilot-secret-2026";

    private static final String S_USERNAME =
        System.getenv("SPRING_DATASOURCE_USERNAME") != null && !System.getenv("SPRING_DATASOURCE_USERNAME").isBlank()
            ? System.getenv("SPRING_DATASOURCE_USERNAME")
            : "tamva_user";

    private static final String S_PASSWORD =
        System.getenv("SPRING_DATASOURCE_PASSWORD") != null && !System.getenv("SPRING_DATASOURCE_PASSWORD").isBlank()
            ? System.getenv("SPRING_DATASOURCE_PASSWORD")
            : "tamva_pass";

    private static final String S_DRIVER_CLASS =
        System.getenv("SPRING_DATASOURCE_DRIVER_CLASS_NAME") != null && !System.getenv("SPRING_DATASOURCE_DRIVER_CLASS_NAME").isBlank()
            ? System.getenv("SPRING_DATASOURCE_DRIVER_CLASS_NAME")
            : "org.postgresql.Driver";

    private static boolean isPortOpen(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        if (isPortOpen("localhost", 5432)) {
            registry.add("spring.datasource.url", () -> DB_URL);
            registry.add("spring.datasource.username", () -> S_USERNAME);
            registry.add("spring.datasource.password", () -> S_PASSWORD);
            registry.add("spring.datasource.driver-class-name",
                    () -> S_DRIVER_CLASS);

            registry.add("spring.flyway.url", () -> DB_URL);
            registry.add("spring.flyway.user", () -> S_USERNAME);
            registry.add("spring.flyway.password", () -> S_PASSWORD);
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:tamvauthdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

            registry.add("spring.flyway.url", () -> "jdbc:h2:mem:tamvauthdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
            registry.add("spring.flyway.user", () -> "sa");
            registry.add("spring.flyway.password", () -> "");
        }

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