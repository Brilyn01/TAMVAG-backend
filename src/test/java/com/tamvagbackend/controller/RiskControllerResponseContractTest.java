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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class
})
class RiskControllerResponseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskEngineService riskEngineService;

    @Test
    @WithMockUser(authorities = "SCOPE_risk:evaluate")
    void successfulRiskEvaluationReturnsStandardResponseContract() throws Exception {

        UUID riskEventId = UUID.randomUUID();

        RiskDtos.RiskEvaluationResponse response =
                new RiskDtos.RiskEvaluationResponse(
                        riskEventId,
                        65,
                        "CHALLENGE",
                        "STEP_UP_AUTHENTICATION",
                        List.of(
                                new RiskDtos.ReasonDetail(
                                        "R003",
                                        "Unusual transaction amount",
                                        "HIGH"
                                )
                        ),
                        List.of("R003"),
                        "rules-2026.09.1",
                        "1.0.0",
                        "feature-snapshot-123",
                        Instant.now().plusSeconds(300)
                );

        when(riskEngineService.evaluateTransaction(any(RiskDtos.RiskEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "response-contract-001")
                        .content("""
                                {
                                  "customer_id": "c96fcb17-8022-4db3-aaf8-38ca09d613f1",
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
                                  "occurred_at": "2026-09-15T10:00:00Z",
                                  "context": {
                                    "authentication_method": "MFA"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "X-Request-Id",
                        "response-contract-001"
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.risk_event_id").value(
                        riskEventId.toString()
                ))
                .andExpect(jsonPath("$.risk_score").value(65))
                .andExpect(jsonPath("$.decision").value("CHALLENGE"))
                .andExpect(jsonPath("$.recommended_action").value(
                        "STEP_UP_AUTHENTICATION"
                ))
                .andExpect(jsonPath("$.reasons").isArray())
                .andExpect(jsonPath("$.reasons[0].code").value("R003"))
                .andExpect(jsonPath("$.reasons[0].label").value(
                        "Unusual transaction amount"
                ))
                .andExpect(jsonPath("$.reasons[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.reason_codes[0]").value("R003"))
                .andExpect(jsonPath("$.ruleset_version").value(
                        "rules-2026.09.1"
                ))
                .andExpect(jsonPath("$.model_version").value("1.0.0"))
                .andExpect(jsonPath("$.feature_snapshot_id").value(
                        "feature-snapshot-123"
                ))
                .andExpect(jsonPath("$.expires_at").exists());
    }
}