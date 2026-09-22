package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.AdminUser;
import com.tamvagbackend.domain.repository.AdminUserRepository;
import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionRequest;
import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProvisioningServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    private PasswordEncoder passwordEncoder;
    private AdminProvisioningService adminProvisioningService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        adminProvisioningService = new AdminProvisioningService(adminUserRepository, passwordEncoder);
    }

    @Test
    void provisionAdminSuccess() {
        AdminProvisionRequest request = new AdminProvisionRequest(
                "analyst@tamva.com",
                "AnalystPassword!2026",
                "Kofi",
                "Annan",
                "ADMIN",
                "RISK_ANALYST"
        );

        when(adminUserRepository.existsByEmailIgnoreCase("analyst@tamva.com")).thenReturn(false);
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> {
            AdminUser u = invocation.getArgument(0);
            u.setAdminUserId(UUID.randomUUID());
            return u;
        });

        AdminProvisionResponse response = adminProvisioningService.provisionAdmin(request);

        assertNotNull(response);
        assertEquals("analyst@tamva.com", response.email());
        assertEquals("RISK_ANALYST", response.operationalRole());
        assertEquals("ACTIVE", response.status());
        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    void provisionAdminDuplicateEmailThrowsConflict() {
        AdminProvisionRequest request = new AdminProvisionRequest(
                "existing@tamva.com",
                "Password!2026",
                "Existing",
                "User",
                "ADMIN",
                "RISK_ANALYST"
        );

        when(adminUserRepository.existsByEmailIgnoreCase("existing@tamva.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> adminProvisioningService.provisionAdmin(request));
    }

    @Test
    void provisionAdminInvalidRoleThrowsBadRequest() {
        AdminProvisionRequest request = new AdminProvisionRequest(
                "test@tamva.com",
                "Password!2026",
                "Test",
                "User",
                "INVALID_ROLE",
                "RISK_ANALYST"
        );

        assertThrows(ResponseStatusException.class, () -> adminProvisioningService.provisionAdmin(request));
    }
}
