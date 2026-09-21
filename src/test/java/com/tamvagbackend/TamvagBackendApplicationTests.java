package com.tamvagbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.Socket;

@SpringBootTest
@ActiveProfiles("test")
class TamvagBackendApplicationTests {

    private static final String DB_URL =
            System.getenv("SPRING_DATASOURCE_URL");

    private static final String DB_USERNAME = System.getenv("SPRING_DATASOURCE_USERNAME");

    private static final String DB_PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        if (!isPortOpen("localhost", 5432)) {
            throw new IllegalStateException(
                    "Local PostgreSQL is not running on localhost:5432."
            );
        }

        registry.add("spring.datasource.url", () -> DB_URL);
        registry.add("spring.datasource.username", () -> DB_USERNAME);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );

        registry.add("spring.flyway.url", () -> DB_URL);
        registry.add("spring.flyway.user", () -> DB_USERNAME);
        registry.add("spring.flyway.password", () -> DB_PASSWORD);
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void contextLoads() {
    }
}
