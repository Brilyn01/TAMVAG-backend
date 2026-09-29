package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.ApplicationDtos;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.security.CallerContext;
import com.tamvagbackend.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class ApplicationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @Test
    void adminCallerWithoutInstitutionIdCanListApplications() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        when(applicationService.list(any(CallerContext.AdminCaller.class)))
                .thenReturn(List.of(
                        new ApplicationDtos.ApplicationResponse(
                                applicationId,
                                institutionId,
                                "tamva_client_1",
                                "Partner App",
                                "ACTIVE",
                                List.of("risk:evaluate"),
                                "2026-09-28T00:00:00Z"
                        )
                ));

        mockMvc.perform(
                get("/v1/applications")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(adminUserId.toString())
                                        .claim("token_type", "admin")
                                        .claim("admin_user_id", adminUserId.toString())
                                        .claim("email", "admin@tamva.com")
                                        .claim("role", "SUPER_ADMIN")
                                        .claim("operational_role", "SECURITY_ADMIN")
                                )
                                .authorities(() -> "SCOPE_application:read")
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].application_id").value(applicationId.toString()))
        .andExpect(jsonPath("$[0].name").value("Partner App"));
    }

    @Test
    void adminCallerWithoutInstitutionIdCanCreateApplication() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        ApplicationDtos.ApplicationResponse appResponse = new ApplicationDtos.ApplicationResponse(
                applicationId,
                institutionId,
                "tamva_client_admin",
                "New App",
                "ACTIVE",
                List.of("risk:evaluate"),
                "2026-09-28T00:00:00Z"
        );

        when(applicationService.create(any(), any(CallerContext.AdminCaller.class)))
                .thenReturn(new ApplicationDtos.CreateApplicationResponse(appResponse, "secret-123"));

        mockMvc.perform(
                post("/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "institution_id": "%s",
                                  "name": "New App",
                                  "scopes": ["risk:evaluate"]
                                }
                                """.formatted(institutionId))
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(adminUserId.toString())
                                        .claim("token_type", "admin")
                                        .claim("admin_user_id", adminUserId.toString())
                                        .claim("email", "admin@tamva.com")
                                        .claim("role", "SUPER_ADMIN")
                                        .claim("operational_role", "SECURITY_ADMIN")
                                )
                                .authorities(() -> "SCOPE_application:manage")
                        )
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.application.application_id").value(applicationId.toString()))
        .andExpect(jsonPath("$.client_secret").value("secret-123"));
    }

    @Test
    void adminCallerWithoutRequiredScopeIsForbidden() throws Exception {
        UUID adminUserId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/applications")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(adminUserId.toString())
                                        .claim("token_type", "admin")
                                        .claim("admin_user_id", adminUserId.toString())
                                        .claim("email", "admin@tamva.com")
                                        .claim("role", "ADMIN")
                                        .claim("operational_role", "AUDITOR")
                                )
                                .authorities(() -> "SCOPE_other:read")
                        )
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void institutionCallerWithInstitutionIdCanListApplications() throws Exception {
        UUID institutionId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(applicationService.list(any(CallerContext.InstitutionCaller.class)))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/v1/applications")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("institution-app")
                                        .claim("token_type", "institution")
                                        .claim("institution_id", institutionId.toString())
                                        .claim("application_id", applicationId.toString())
                                )
                                .authorities(() -> "SCOPE_application:read")
                        )
        )
        .andExpect(status().isOk());
    }
}
