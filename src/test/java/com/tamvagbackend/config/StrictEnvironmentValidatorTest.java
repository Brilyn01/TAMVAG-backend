package com.tamvagbackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrictEnvironmentValidatorTest {

    @Test
    void validationFailsWhenJwtSecretIsDefault() {
        StrictEnvironmentValidator validator = new StrictEnvironmentValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost:5432/tamva");
        ReflectionTestUtils.setField(validator, "dbPassword", "securepass123");
        ReflectionTestUtils.setField(validator, "jwtSecret", "change-this-development-secret-to-a-long-random-value-2026");
        ReflectionTestUtils.setField(validator, "webhookEncryptionKey", "valid-key-123456789012345678901234");

        assertThrows(IllegalStateException.class, validator::validateStrictEnvironment);
    }

    @Test
    void validationPassesWhenAllProductionSecretsAreValid() {
        StrictEnvironmentValidator validator = new StrictEnvironmentValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost:5432/tamva");
        ReflectionTestUtils.setField(validator, "dbPassword", "securepass123");
        ReflectionTestUtils.setField(validator, "jwtSecret", "261edf5ef58ff5e64455170c974783a97d66759bbc1ffb3a66ccd632f217b031");
        ReflectionTestUtils.setField(validator, "webhookEncryptionKey", "fe255c8e0d3ed0f9cee37296e0e50ae8");

        assertDoesNotThrow(validator::validateStrictEnvironment);
    }
}