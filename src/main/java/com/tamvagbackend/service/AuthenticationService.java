package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Application;
import com.tamvagbackend.domain.repository.ApplicationRepository;
import com.tamvagbackend.dto.AuthDtos.TokenRequest;
import com.tamvagbackend.dto.AuthDtos.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    private final long accessTokenTtlSeconds;

    public AuthenticationService(
            ApplicationRepository applicationRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${tamva.security.access-token-ttl-seconds:3600}")
            long accessTokenTtlSeconds
    ) {
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public TokenResponse authenticate(TokenRequest request) {

        Application application = applicationRepository
                .findByClientId(request.clientId())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid client credentials")
                );

        if (!"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new BadCredentialsException("Application is inactive");
        }

        if (application.getClientSecretHash() == null ||
                !passwordEncoder.matches(
                        request.clientSecret(),
                        application.getClientSecretHash()
                )) {

            throw new BadCredentialsException("Invalid client credentials");
        }

        List<String> scopes = parseScopes(application.getScopes());

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(accessTokenTtlSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tamva")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(application.getClientId())
                .claim("application_id",
                        application.getApplicationId().toString())
                .claim("institution_id",
                        application.getInstitution().getInstitutionId().toString())
                .claim("scope", String.join(" ", scopes))
                .build();

        String accessToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        return new TokenResponse(
                accessToken,
                "Bearer",
                accessTokenTtlSeconds,
                String.join(" ", scopes)
        );
    }

    private List<String> parseScopes(String scopes) {

        if (scopes == null || scopes.isBlank()) {
            return List.of();
        }

        String normalized = scopes
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        if (normalized.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toList());
    }
}