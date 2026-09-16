package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.RiskDtos;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class
})
class RiskControllerRequestIdContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskEngineService riskEngineService;

    @Test
    @WithMockUser(authorities = "SCOPE_risk:evaluate")
    void successfulResponsePreservesSuppliedRequestId() throws Exception {

        RiskDtos.RiskEvaluationResponse response =
                new RiskDtos.RiskEvaluationResponse(
                        UUID.randomUUID(),
                        20,
                        "ALLOW",
                        "PROCEED",
                        List.of(),
                        List.of(),
                        "rules-2026.09.1",
                        "1.0.0",
                        "feature-snapshot-123",
                        Instant.now().plusSeconds(300)
                );

        when(riskEngineService.evaluateTransaction(
                any(RiskDtos.RiskEvaluationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "success-request-123")
                        .content("""
                                {
                                  "customer_id": "c96fcb17-8022-4db3-aaf8-38ca09d613f1",
                                  "account_id": null,
                                  "amount": 500.00,
                                  "currency": "GHS",
                                  "destination": {
                                    "type": "MOBILE_MONEY",
                                    "identifier": "0240001122",
                                    "reference": "REF123"
                                  },
                                  "device_id": "dev_fingerprint_999",
                                  "channel": "MOBILE_APP",
                                  "occurred_at": "2026-09-15T10:00:00Z",
                                  "context": {
                                    "authentication_method": "MFA"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "X-Request-Id",
                        "success-request-123"
                ));
    }
}