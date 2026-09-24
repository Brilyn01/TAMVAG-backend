package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.AdminUser;
import com.tamvagbackend.domain.repository.AdminUserRepository;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginRequest;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginResponse;
import com.tamvagbackend.security.RolePermissions;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    private PasswordEncoder passwordEncoder;
    private AdminAuthenticationService adminAuthenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        adminAuthenticationService = new AdminAuthenticationService(
                adminUserRepository,
                passwordEncoder,
                jwtEncoder,
                3600
        );
    }

    @Test
    void loginSuccess() {
        AdminUser admin = new AdminUser();
        admin.setAdminUserId(UUID.randomUUID());
        admin.setEmail("analyst@tamva.com");
        admin.setPasswordHash(passwordEncoder.encode("Secret!2026"));
        admin.setFirstName("Kofi");
        admin.setLastName("Annan");
        admin.setRole("ADMIN");
        admin.setOperationalRole("RISK_ANALYST");
        admin.setStatus("ACTIVE");

        when(adminUserRepository.findByEmailIgnoreCase("analyst@tamva.com")).thenReturn(Optional.of(admin));

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("admin.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        AdminLoginResponse response = adminAuthenticationService.login(
                new AdminLoginRequest("analyst@tamva.com", "Secret!2026")
        );

        assertNotNull(response);
        assertEquals("admin.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
        assertEquals("analyst@tamva.com", response.adminUser().email());
        assertTrue(response.permissions().contains(RolePermissions.CASES_READ));
        assertTrue(response.permissions().contains(RolePermissions.CASES_WRITE));
    }

    @Test
    void loginInvalidPasswordThrowsBadCredentials() {
        AdminUser admin = new AdminUser();
        admin.setEmail("analyst@tamva.com");
        admin.setPasswordHash(passwordEncoder.encode("Secret!2026"));
        admin.setStatus("ACTIVE");
        admin.setOperationalRole("RISK_ANALYST");

        when(adminUserRepository.findByEmailIgnoreCase("analyst@tamva.com")).thenReturn(Optional.of(admin));

        assertThrows(BadCredentialsException.class, () ->
                adminAuthenticationService.login(new AdminLoginRequest("analyst@tamva.com", "WrongPassword"))
        );
    }

    @Test
    void loginInactiveAdminThrowsBadCredentials() {
        AdminUser admin = new AdminUser();
        admin.setEmail("inactive@tamva.com");
        admin.setPasswordHash(passwordEncoder.encode("Secret!2026"));
        admin.setStatus("SUSPENDED");
        admin.setOperationalRole("RISK_ANALYST");

        when(adminUserRepository.findByEmailIgnoreCase("inactive@tamva.com")).thenReturn(Optional.of(admin));

        assertThrows(BadCredentialsException.class, () ->
                adminAuthenticationService.login(new AdminLoginRequest("inactive@tamva.com", "Secret!2026"))
        );
    }
}
