package com.tamvagbackend.controller;

import com.tamvagbackend.dto.RiskDtos;
import com.tamvagbackend.service.RiskEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskController.class)
@Import(com.tamvagbackend.config.SecurityConfig.class)
class RiskControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskEngineService riskEngineService;

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_profile:read")
    void requestWithoutRequiredScopeIsForbidden() throws Exception {
        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
        .andExpect(status().isForbidden());
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