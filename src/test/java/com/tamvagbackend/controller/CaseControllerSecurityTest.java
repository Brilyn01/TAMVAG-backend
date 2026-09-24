package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.CaseManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CaseController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class CaseControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaseManagementService caseManagementService;

    @Test
    void requestWithoutTokenIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(
                get("/v1/cases")
                        .header("X-Request-Id", "case-401-test")
        )
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(
                "X-Request-Id",
                "case-401-test"
        ))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message")
                .value("Authentication is required"))
        .andExpect(jsonPath("$.path")
                .value("/v1/cases"))
        .andExpect(jsonPath("$.request_id")
                .value("case-401-test"));
    }

    @Test
    void requestWithoutRequiredScopeIsRejectedWithForbidden()
            throws Exception {

        mockMvc.perform(
                get("/v1/cases")
                        .header("X-Request-Id", "case-403-test")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("institution-actor")
                                        .claim(
                                                "token_type",
                                                "institution"
                                        )
                                        .claim(
                                                "institution_id",
                                                UUID.randomUUID().toString()
                                        )
                                        .claim(
                                                "application_id",
                                                UUID.randomUUID().toString()
                                        )
                                )
                                .authorities(
                                        () -> "SCOPE_profile:read"
                                )
                        )
        )
        .andExpect(status().isForbidden())
        .andExpect(header().string(
                "X-Request-Id",
                "case-403-test"
        ))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.message")
                .value("Access denied"))
        .andExpect(jsonPath("$.path")
                .value("/v1/cases"))
        .andExpect(jsonPath("$.request_id")
                .value("case-403-test"));
    }

    @Test
    void requestWithRequiredReadScopeIsAllowed()
            throws Exception {

        UUID institutionId = UUID.randomUUID();

        when(caseManagementService.getCases(
                null,
                null,
                institutionId
        )).thenReturn(List.of());

        mockMvc.perform(
                get("/v1/cases")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("institution-actor")
                                        .claim(
                                                "token_type",
                                                "institution"
                                        )
                                        .claim(
                                                "institution_id",
                                                institutionId.toString()
                                        )
                                        .claim(
                                                "application_id",
                                                UUID.randomUUID().toString()
                                        )
                                )
                                .authorities(
                                        () -> "SCOPE_cases:read"
                                )
                        )
        )
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    }

    @Test
    void requestWithReadScopeCannotPerformWriteAction()
            throws Exception {

        UUID institutionId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        mockMvc.perform(
                org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders
                        .patch("/v1/cases/" + caseId)
                        .header("Content-Type", "application/json")
                        .content("""
                                {
                                  "action": "INVESTIGATE",
                                  "disposition": null,
                                  "assignee": null,
                                  "notes": "Investigation started"
                                }
                                """)
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("institution-actor")
                                        .claim(
                                                "token_type",
                                                "institution"
                                        )
                                        .claim(
                                                "institution_id",
                                                institutionId.toString()
                                        )
                                        .claim(
                                                "application_id",
                                                UUID.randomUUID().toString()
                                        )
                                )
                                .authorities(
                                        () -> "SCOPE_cases:read"
                                )
                        )
        )
        .andExpect(status().isForbidden());
    }
}