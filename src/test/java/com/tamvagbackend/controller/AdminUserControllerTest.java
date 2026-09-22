package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionResponse;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.AdminProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminProvisioningService adminProvisioningService;

    @Test
    void unauthenticatedProvisioningIsRejected() throws Exception {
        mockMvc.perform(
                post("/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "analyst@tamva.com",
                                  "password": "AnalystPassword!2026",
                                  "firstName": "Kofi",
                                  "lastName": "Annan",
                                  "role": "ADMIN",
                                  "operationalRole": "RISK_ANALYST"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void provisionWithoutAdminProvisionScopeIsForbidden() throws Exception {
        mockMvc.perform(
                post("/v1/admin/users")
                        .with(jwt().jwt(builder -> builder
                                .subject(UUID.randomUUID().toString())
                                .claim("token_type", "admin")
                                .claim("admin_user_id", UUID.randomUUID().toString())
                                .claim("role", "ADMIN")
                                .claim("operational_role", "RISK_ANALYST")
                                .claim("scope", "cases:read cases:write")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "analyst@tamva.com",
                                  "password": "AnalystPassword!2026",
                                  "firstName": "Kofi",
                                  "lastName": "Annan",
                                  "role": "ADMIN",
                                  "operationalRole": "RISK_ANALYST"
                                }
                                """)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void authorizedSuperAdminCanProvisionAdmin() throws Exception {
        UUID newAdminId = UUID.randomUUID();
        when(adminProvisioningService.provisionAdmin(any())).thenReturn(
                new AdminProvisionResponse(
                        newAdminId,
                        "analyst@tamva.com",
                        "Kofi",
                        "Annan",
                        "ADMIN",
                        "RISK_ANALYST",
                        "ACTIVE",
                        "Admin account provisioned successfully"
                )
        );

        mockMvc.perform(
                post("/v1/admin/users")
                        .with(jwt().jwt(builder -> builder
                                .subject(UUID.randomUUID().toString())
                                .claim("token_type", "admin")
                                .claim("admin_user_id", UUID.randomUUID().toString())
                                .claim("role", "SUPER_ADMIN")
                                .claim("operational_role", "SECURITY_ADMINISTRATOR")
                                .claim("scope", "admin:provision admin:manage")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "analyst@tamva.com",
                                  "password": "AnalystPassword!2026",
                                  "firstName": "Kofi",
                                  "lastName": "Annan",
                                  "role": "ADMIN",
                                  "operationalRole": "RISK_ANALYST"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.adminUserId").value(newAdminId.toString()))
        .andExpect(jsonPath("$.email").value("analyst@tamva.com"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
