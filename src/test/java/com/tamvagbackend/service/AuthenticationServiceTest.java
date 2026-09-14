package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Application;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.repository.ApplicationRepository;
import com.tamvagbackend.dto.AuthDtos.TokenRequest;
import com.tamvagbackend.dto.AuthDtos.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;

    private Application application;
    private UUID applicationId;
    private UUID institutionId;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        applicationId = UUID.randomUUID();
        institutionId = UUID.randomUUID();

        Institution institution = new Institution();
        institution.setInstitutionId(institutionId);

        application = new Application();
        application.setApplicationId(applicationId);
        application.setInstitution(institution);
        application.setClientId("app_test");
        application.setClientSecretHash(
                passwordEncoder.encode("test-secret")
        );
        application.setName("Test Application");
        application.setStatus("ACTIVE");
        application.setScopes(
                "[\"risk:evaluate\", \"profile:read\"]"
        );

        authenticationService = new AuthenticationService(
                applicationRepository,
                passwordEncoder,
                jwtEncoder,
                3600
        );
    }

    @Test
    void validCredentialsIssueAccessToken() {
        when(applicationRepository.findByClientId("app_test"))
                .thenReturn(Optional.of(application));

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("scope", "risk:evaluate profile:read")
                .subject("app_test")
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        TokenResponse response = authenticationService.authenticate(
                new TokenRequest("app_test", "test-secret")
        );

        assertNotNull(response);
        assertEquals("test-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
        assertEquals(
                "risk:evaluate profile:read",
                response.scope()
        );
    }

    @Test
    void invalidSecretIsRejected() {
        when(applicationRepository.findByClientId("app_test"))
                .thenReturn(Optional.of(application));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticate(
                        new TokenRequest("app_test", "wrong-secret")
                )
        );
    }

    @Test
    void unknownClientIsRejected() {
        when(applicationRepository.findByClientId("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticate(
                        new TokenRequest("unknown", "test-secret")
                )
        );
    }

    @Test
    void inactiveApplicationIsRejected() {
        application.setStatus("INACTIVE");

        when(applicationRepository.findByClientId("app_test"))
                .thenReturn(Optional.of(application));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticate(
                        new TokenRequest("app_test", "test-secret")
                )
        );
    }

    @Test
    void applicationWithoutSecretIsRejected() {
        application.setClientSecretHash(null);

        when(applicationRepository.findByClientId("app_test"))
                .thenReturn(Optional.of(application));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticate(
                        new TokenRequest("app_test", "test-secret")
                )
        );
    }
}