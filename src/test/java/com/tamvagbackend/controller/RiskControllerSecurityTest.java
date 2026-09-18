package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.RiskEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class
})
class RiskControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskEngineService riskEngineService;

    @Test
    void requestWithoutTokenIsRejectedWithApiError() throws Exception {
        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "security-401-test")
                        .content(validRequest())
        )
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(
                "X-Request-Id",
                "security-401-test"
        ))
        .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
        ))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message")
                .value("Authentication is required"))
        .andExpect(jsonPath("$.path")
                .value("/v1/risk/evaluate"))
        .andExpect(jsonPath("$.request_id")
                .value("security-401-test"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_profile:read")
    void requestWithoutRequiredScopeIsForbiddenWithApiError()
            throws Exception {

        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "security-403-test")
                        .content(validRequest())
        )
        .andExpect(status().isForbidden())
        .andExpect(header().string(
                "X-Request-Id",
                "security-403-test"
        ))
        .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
        ))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.message")
                .value("Access denied"))
        .andExpect(jsonPath("$.path")
                .value("/v1/risk/evaluate"))
        .andExpect(jsonPath("$.request_id")
                .value("security-403-test"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_risk:evaluate")
    void requestWithRequiredScopeIsAllowed() throws Exception {
        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
        .andExpect(status().isOk());
    }

    private String validRequest() {
        UUID customerId = UUID.randomUUID();

        return """
                {
                  "customer_id": "%s",
                  "account_id": null,
                  "amount": 8500.00,
                  "currency": "GHS",
                  "destination": {
                    "type": "MOBILE_MONEY",
                    "identifier": "0240001122",
                    "reference": "REF123"
                  },
                  "device_id": "dev_fingerprint_999",
                  "channel": "MOBILE_APP",
                  "occurred_at": "%s",
                  "context": {
                    "authentication_method": "MFA"
                  }
                }
                """.formatted(customerId, Instant.now());
    }
}