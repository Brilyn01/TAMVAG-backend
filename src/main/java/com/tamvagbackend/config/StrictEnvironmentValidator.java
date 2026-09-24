package com.tamvagbackend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"staging", "prod"})
public class StrictEnvironmentValidator {

    private static final Logger log = LoggerFactory.getLogger(StrictEnvironmentValidator.class);

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${tamva.security.jwt-secret:}")
    private String jwtSecret;

    @Value("${tamva.webhook.encryption-key:}")
    private String webhookEncryptionKey;

    @PostConstruct
    public void validateStrictEnvironment() {
        log.info("Executing strict environment safety checks for staging/production...");

        if (dbUrl == null || dbUrl.isBlank() || dbUrl.contains("h2:mem")) {
            throw new IllegalStateException(
                "CRITICAL SECURITY CONFIGURATION ERROR: Staging and Production environments require a valid PostgreSQL database URL."
            );
        }

        if (dbPassword == null || dbPassword.isBlank() || "tamva_pass".equalsIgnoreCase(dbPassword.trim())) {
            throw new IllegalStateException(
                "CRITICAL SECURITY CONFIGURATION ERROR: Default development database password detected in staging/production."
            );
        }

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.contains("change-this")) {
            throw new IllegalStateException(
                "CRITICAL SECURITY CONFIGURATION ERROR: Insecure or default JWT secret detected in staging/production."
            );
        }

        if (webhookEncryptionKey == null || webhookEncryptionKey.isBlank() || webhookEncryptionKey.contains("change-this")) {
            throw new IllegalStateException(
                "CRITICAL SECURITY CONFIGURATION ERROR: Insecure or default Webhook Encryption Key detected in staging/production."
            );
        }

        log.info("Strict environment safety checks passed successfully!");
    }
}