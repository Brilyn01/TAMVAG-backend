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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class
})
class RiskControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskEngineService riskEngineService;

    @Test
    @WithMockUser(authorities = "SCOPE_risk:evaluate")
    void missingRequiredFieldsReturnStandardApiError() throws Exception {
        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "validation-test-001")
                        .content("""
                                {
                                  "amount": 8500.00,
                                  "currency": "GHS"
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(header().string(
                "X-Request-Id",
                "validation-test-001"
        ))
        .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
        ))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.path").value("/v1/risk/evaluate"))
        .andExpect(jsonPath("$.request_id").value("validation-test-001"))
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.details[?(@.field == 'customerId')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'amount')]").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_risk:evaluate")
    void invalidAmountReturnsValidationError() throws Exception {
        mockMvc.perform(
                post("/v1/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "validation-test-002")
                        .content("""
                                {
                                  "customer_id": "c96fcb17-8022-4db3-aaf8-38ca09d613f1",
                                  "account_id": null,
                                  "amount": 0,
                                  "currency": "GHS",
                                  "destination": {
                                    "type": "MOBILE_MONEY",
                                    "identifier": "0240001122",
                                    "reference": "REF123"
                                  },
                                  "device_id": "dev_fingerprint_999",
                                  "channel": "MOBILE_APP",
                                  "context": {
                                    "authentication_method": "MFA"
                                  }
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(header().string(
                "X-Request-Id",
                "validation-test-002"
        ))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.request_id").value("validation-test-002"))
        .andExpect(jsonPath("$.details").isArray());
    }
}