package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.RefreshTokenSession;
import com.tamvagbackend.domain.entity.TamvaUser;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.RefreshTokenSessionRepository;
import com.tamvagbackend.domain.repository.TamvaUserRepository;
import com.tamvagbackend.dto.UserAuthDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private TamvaUserRepository tamvaUserRepository;

    @Mock
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    private PasswordEncoder passwordEncoder;
    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userAuthService = new UserAuthService(
                tamvaUserRepository,
                refreshTokenSessionRepository,
                customerRepository,
                passwordEncoder,
                jwtEncoder,
                900,
                7
        );
    }

    @Test
    void signUpSuccess() {
        SignUpRequest request = new SignUpRequest(
                "ama.mensah@example.com",
                "StrongPassword!2026",
                "Ama",
                "Mensah",
                "+233240000000"
        );

        when(tamvaUserRepository.existsByEmailIgnoreCase("ama.mensah@example.com")).thenReturn(false);

        Customer customer = new Customer(UUID.randomUUID(), "ama.mensah@example.com", "INDIVIDUAL", "ACTIVE");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        when(tamvaUserRepository.save(any(TamvaUser.class))).thenAnswer(invocation -> {
            TamvaUser u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });

        SignUpResponse response = userAuthService.signUp(request);

        assertNotNull(response);
        assertEquals("ama.mensah@example.com", response.email());
        assertEquals("Ama", response.firstName());
        assertEquals("Mensah", response.lastName());
        assertEquals("PENDING_VERIFICATION", response.status());
        assertNotNull(response.customerId());
        verify(tamvaUserRepository).save(any(TamvaUser.class));
    }

    @Test
    void signUpDuplicateEmailThrowsConflict() {
        SignUpRequest request = new SignUpRequest(
                "existing@example.com",
                "StrongPassword!2026",
                "Existing",
                "User",
                null
        );

        when(tamvaUserRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> userAuthService.signUp(request));
    }

    @Test
    void signInSuccess() {
        SignInRequest request = new SignInRequest("ama.mensah@example.com", "StrongPassword!2026");

        TamvaUser user = new TamvaUser();
        user.setUserId(UUID.randomUUID());
        user.setEmail("ama.mensah@example.com");
        user.setPasswordHash(passwordEncoder.encode("StrongPassword!2026"));
        user.setFirstName("Ama");
        user.setLastName("Mensah");
        user.setStatus("ACTIVE");

        when(tamvaUserRepository.findByEmailIgnoreCase("ama.mensah@example.com")).thenReturn(Optional.of(user));

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        SignInResponse response = userAuthService.signIn(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(900, response.expiresIn());
        assertEquals("ama.mensah@example.com", response.user().email());
        verify(refreshTokenSessionRepository).save(any(RefreshTokenSession.class));
    }

    @Test
    void signInInvalidPasswordThrowsBadCredentials() {
        SignInRequest request = new SignInRequest("ama.mensah@example.com", "WrongPassword");

        TamvaUser user = new TamvaUser();
        user.setEmail("ama.mensah@example.com");
        user.setPasswordHash(passwordEncoder.encode("CorrectPassword"));

        when(tamvaUserRepository.findByEmailIgnoreCase("ama.mensah@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> userAuthService.signIn(request));
    }

    @Test
    void signInUnknownEmailThrowsBadCredentials() {
        SignInRequest request = new SignInRequest("unknown@example.com", "Password");

        when(tamvaUserRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> userAuthService.signIn(request));
    }

    @Test
    void refreshSuccess() {
        RefreshRequest request = new RefreshRequest("sample-raw-refresh-token");

        TamvaUser user = new TamvaUser();
        user.setUserId(UUID.randomUUID());
        user.setEmail("ama.mensah@example.com");
        user.setStatus("ACTIVE");

        RefreshTokenSession session = new RefreshTokenSession();
        session.setUser(user);
        session.setRevoked(false);
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        when(refreshTokenSessionRepository.findByTokenHash(anyString())).thenReturn(Optional.of(session));

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("new.mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        RefreshResponse response = userAuthService.refresh(request);

        assertNotNull(response);
        assertEquals("new.mock.jwt.token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertTrue(session.isRevoked());
        verify(refreshTokenSessionRepository, times(2)).save(any(RefreshTokenSession.class));
    }

    @Test
    void refreshRevokedTokenThrowsBadCredentials() {
        RefreshRequest request = new RefreshRequest("revoked-token");

        RefreshTokenSession session = new RefreshTokenSession();
        session.setRevoked(true);
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        when(refreshTokenSessionRepository.findByTokenHash(anyString())).thenReturn(Optional.of(session));

        assertThrows(BadCredentialsException.class, () -> userAuthService.refresh(request));
    }
}
