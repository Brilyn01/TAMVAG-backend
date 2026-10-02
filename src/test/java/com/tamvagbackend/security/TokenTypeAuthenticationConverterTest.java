package com.tamvagbackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenTypeAuthenticationConverterTest {

    private final TokenTypeAuthenticationConverter converter =
            new TokenTypeAuthenticationConverter();

    @Test
    void validUserTokenWithCustomerIdIsAccepted() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Jwt jwt = jwt(
                Map.of(
                        "token_type", "user",
                        "user_id", userId.toString(),
                        "customer_id", customerId.toString(),
                        "scope", "profile:read wallet:read"
                )
        );

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertEquals(2, authentication.getAuthorities().size());

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("SCOPE_profile:read")));

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("SCOPE_wallet:read")));
    }

    @Test
    void userTokenWithoutCustomerIdIsRejected() {
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(
                Map.of(
                        "token_type", "user",
                        "user_id", userId.toString(),
                        "scope", "profile:read"
                )
        );

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> converter.convert(jwt)
        );

        assertEquals(
                "User token requires customer_id",
                exception.getMessage()
        );
    }

    @Test
    void unsupportedTokenTypeIsRejected() {
        Jwt jwt = jwt(
                Map.of(
                        "token_type", "unknown",
                        "user_id", UUID.randomUUID().toString()
                )
        );

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> converter.convert(jwt)
        );

        assertEquals(
                "Unsupported token type",
                exception.getMessage()
        );
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claims(existing -> existing.putAll(claims))
                .build();
    }
}