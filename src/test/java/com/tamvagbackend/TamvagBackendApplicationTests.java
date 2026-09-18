package com.tamvagbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.Socket;

@SpringBootTest
@ActiveProfiles("test")
class TamvagBackendApplicationTests {

    private static PostgreSQLContainer<?> postgresContainer;

    static {
        if (!isPortOpen("localhost", 5432)) {
            try {
                postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("tamvadb")
                        .withUsername("tamva")
                        .withPassword("tamva");
                postgresContainer.start();
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgresContainer::getUsername);
            registry.add("spring.datasource.password", postgresContainer::getPassword);
            registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);

            registry.add("spring.flyway.url", postgresContainer::getJdbcUrl);
            registry.add("spring.flyway.user", postgresContainer::getUsername);
            registry.add("spring.flyway.password", postgresContainer::getPassword);
        } else if (isPortOpen("localhost", 5432)) {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/tamva");
            registry.add("spring.datasource.username", () -> "tamva_user");
            registry.add("spring.datasource.password", () -> "tamva_pass");
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

            registry.add("spring.flyway.url", () -> "jdbc:postgresql://localhost:5432/tamva");
            registry.add("spring.flyway.user", () -> "tamva_user");
            registry.add("spring.flyway.password", () -> "tamva_pass");
        }
    }

    @Test
    void contextLoads() {
    }
}