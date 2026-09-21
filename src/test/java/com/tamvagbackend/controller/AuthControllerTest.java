package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.AuthDtos.TokenResponse;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    String seedClientSecret = System.getenv("TAMVA_SEED_CLIENT_SECRET");

    @Test
    void validCredentialsReturnAccessToken() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenReturn(
                        new TokenResponse(
                                "test-access-token",
                                "Bearer",
                                3600,
                                "risk:evaluate profile:read"
                        )
                );

        mockMvc.perform(
                post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "auth-200-test")
                        .content("""
                                {
                                  "clientId": "app_gcb_pilot_2026",
                                  "clientSecret": "%s"
                                }
                                """.formatted(seedClientSecret))
        )
        .andExpect(status().isOk())
        .andExpect(header().string(
                "X-Request-Id",
                "auth-200-test"
        ))
        .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
        ))
        .andExpect(jsonPath("$.accessToken").value("test-access-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(jsonPath("$.scope")
                .value("risk:evaluate profile:read"));
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenThrow(
                        new BadCredentialsException(
                                "Invalid client credentials"
                        )
                );

        mockMvc.perform(
                post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "auth-401-test")
                        .content("""
                                {
                                  "clientId": "app_gcb_pilot_2026",
                                  "clientSecret": "definitely-wrong-secret"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(
                "X-Request-Id",
                "auth-401-test"
        ))
        .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
        ))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message")
                .value("Invalid client credentials"))
        .andExpect(jsonPath("$.path")
                .value("/v1/auth/token"))
        .andExpect(jsonPath("$.request_id")
                .value("auth-401-test"));
    }
}