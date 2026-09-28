package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginResponse;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLogoutResponse;
import com.tamvagbackend.dto.AdminAuthDtos.AdminUserInfo;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.AdminAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuthenticationService adminAuthenticationService;

    // ── Login Tests ──────────────────────────────────────────────────────

    @Test
    void validCredentialsReturnAdminToken() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(adminAuthenticationService.login(any())).thenReturn(
                new AdminLoginResponse(
                        "admin-access-token",
                        "Bearer",
                        3600,
                        new AdminUserInfo(adminId, "admin@tamva.com", "Admin", "User", "ADMIN", "RISK_ANALYST", "ACTIVE"),
                        Set.of("cases:read", "cases:write")
                )
        );

        mockMvc.perform(
                post("/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@tamva.com",
                                  "password": "AdminPassword!2026"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("admin-access-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.adminUser.email").value("admin@tamva.com"));
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        when(adminAuthenticationService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid admin credentials"));

        mockMvc.perform(
                post("/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@tamva.com",
                                  "password": "WrongPassword"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized());
    }

    // ── Logout Tests ─────────────────────────────────────────────────────

    @Test
    void logoutReturnsOk() throws Exception {
        when(adminAuthenticationService.logout())
                .thenReturn(new AdminLogoutResponse("Admin logged out successfully"));

        mockMvc.perform(
                post("/v1/admin/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Admin logged out successfully"));
    }
}



