package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.CaseDtos.CaseResponse;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.CaseManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCaseController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class AdminCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaseManagementService caseManagementService;

    @Test
    void unauthenticatedCaseAccessIsRejected() throws Exception {
        mockMvc.perform(get("/v1/admin/cases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authorizedAdminCanListCases() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(caseManagementService.getAdminCases(any(), any())).thenReturn(
                List.of(new CaseResponse(
                        caseId,
                        "RISK_EVENT",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        75,
                        "HOLD",
                        "High velocity anomaly",
                        "Multiple transfers within 5 minutes",
                        "HIGH",
                        "HIGH",
                        "OPEN",
                        "RULES_ENGINE",
                        null,
                        "SYSTEM",
                        null,
                        null,
                        null,
                        Instant.now(),
                        Instant.now(),
                        null
                ))
        );

        mockMvc.perform(
                get("/v1/admin/cases")
                        .with(jwt().jwt(builder -> builder
                                .subject(UUID.randomUUID().toString())
                                .claim("token_type", "admin")
                                .claim("admin_user_id", UUID.randomUUID().toString())
                                .claim("role", "ADMIN")
                                .claim("operational_role", "RISK_ANALYST")
                                .claim("scope", "cases:read")
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].case_id").value(caseId.toString()))
        .andExpect(jsonPath("$[0].title").value("High velocity anomaly"));
    }
}
