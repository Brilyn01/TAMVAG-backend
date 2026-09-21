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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public TokenResponse authenticate(TokenRequest request) {

        if (request == null
                || request.clientId() == null
                || request.clientId().isBlank()
                || request.clientSecret() == null
                || request.clientSecret().isBlank()) {

            throw new BadCredentialsException(
                    "Invalid client credentials"
            );
        }

        Application application = applicationRepository
                .findByClientId(request.clientId())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid client credentials"
                        )
                );

        if (!"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new BadCredentialsException(
                    "Application is inactive"
            );
        }

        if (application.getClientSecretHash() == null
                || !passwordEncoder.matches(
                        request.clientSecret(),
                        application.getClientSecretHash()
                )) {

            throw new BadCredentialsException(
                    "Invalid client credentials"
            );
        }

        if (application.getInstitution() == null
                || application.getInstitution().getInstitutionId() == null) {

            throw new IllegalStateException(
                    "Application is not associated with an institution"
            );
        }

        List<String> scopes = parseScopes(
                application.getScopes()
        );

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(
                accessTokenTtlSeconds
        );

        String institutionId = application.getInstitution()
                .getInstitutionId()
                .toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tamva")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(application.getClientId())
                .claim("token_type", "institution")
                .claim(
                        "application_id",
                        application.getApplicationId().toString()
                )
                .claim(
                        "institution_id",
                        institutionId
                )
                .claim(
                        "scope",
                        String.join(" ", scopes)
                )
                .build();

        String accessToken = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                org.springframework.security.oauth2.jwt.JwsHeader
                                        .with(MacAlgorithm.HS256)
                                        .build(),
                                claims
                        )
                )
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