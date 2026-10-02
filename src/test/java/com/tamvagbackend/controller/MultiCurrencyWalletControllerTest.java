package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.MultiCurrencyDtos.*;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.MultiCurrencyWalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MultiCurrencyWalletController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class MultiCurrencyWalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MultiCurrencyWalletService walletService;

    @Test
    void walletDetailsUnauthenticatedReturnsUnauthorized() throws Exception {

        UUID customerId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/wallet/{customerId}", customerId)
        )
        .andExpect(status().isUnauthorized());

        verifyNoInteractions(walletService);
    }

    @Test
    void walletDetailsWithoutWalletReadScopeReturnsForbidden() throws Exception {

        UUID customerId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/wallet/{customerId}", customerId)
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim("customer_id", customerId.toString())
                        ))
        )
        .andExpect(status().isForbidden());

        verifyNoInteractions(walletService);
    }

    @Test
    void walletDetailsWithWrongCustomerReturnsForbidden() throws Exception {

        UUID requestedCustomerId = UUID.randomUUID();
        UUID authenticatedCustomerId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/wallet/{customerId}", requestedCustomerId)
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        authenticatedCustomerId.toString()
                                )
                                .claim("scope", "wallet:read")
                        ))
        )
        .andExpect(status().isForbidden());

        verifyNoInteractions(walletService);
    }

    @Test
    void walletDetailsWithMatchingCustomerAndScopeReturnsOk()
            throws Exception {

        UUID customerId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        MultiCurrencyWalletResponse response =
                new MultiCurrencyWalletResponse(
                        walletId,
                        customerId,
                        "GHS",
                        "GHS",
                        new BigDecimal("1500.00"),
                        List.of(),
                        List.of()
                );

        when(walletService.getWalletDetails(
                eq(customerId),
                eq("GHS")
        )).thenReturn(response);

        mockMvc.perform(
                get("/v1/wallet/{customerId}", customerId)
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        customerId.toString()
                                )
                                .claim("scope", "wallet:read")
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wallet_id").value(walletId.toString()))
        .andExpect(jsonPath("$.customer_id").value(customerId.toString()))
        .andExpect(jsonPath("$.default_currency").value("GHS"))
        .andExpect(jsonPath("$.display_currency").value("GHS"));

        verify(walletService)
                .getWalletDetails(customerId, "GHS");
    }

    @Test
    void walletDetailsWithNonUserTokenReturnsForbidden()
            throws Exception {

        UUID customerId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/wallet/{customerId}", customerId)
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "admin")
                                .claim(
                                        "customer_id",
                                        customerId.toString()
                                )
                                .claim("scope", "wallet:read")
                        ))
        )
        .andExpect(status().isForbidden());

        verifyNoInteractions(walletService);
    }

    @Test
    void walletDetailsWithMissingCustomerIdClaimReturnsForbidden()
            throws Exception {

        UUID customerId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/wallet/{customerId}", customerId)
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim("scope", "wallet:read")
                        ))
        )
        .andExpect(status().isForbidden());

        verifyNoInteractions(walletService);
    }

    @Test
    void transferWithoutAuthenticationReturnsUnauthorized()
            throws Exception {

        UUID customerId = UUID.randomUUID();

        mockMvc.perform(
                post("/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(customerId))
        )
        .andExpect(status().isUnauthorized());

        verifyNoInteractions(walletService);
    }

    @Test
    void transferWithoutWalletWriteScopeReturnsForbidden()
            throws Exception {

        UUID customerId = UUID.randomUUID();

        mockMvc.perform(
                post("/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(customerId))
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        customerId.toString()
                                )
                        ))
        )
        .andExpect(status().isForbidden());

        verifyNoInteractions(walletService);
    }

    @Test
    void transferForWrongCustomerReturnsForbidden()
            throws Exception {

        UUID requestedCustomerId = UUID.randomUUID();
        UUID authenticatedCustomerId = UUID.randomUUID();

        mockMvc.perform(
                post("/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(requestedCustomerId))
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        authenticatedCustomerId.toString()
                                )
                                .claim("scope", "wallet:write")
                        ))
        )
        .andExpect(status().isForbidden());

        verifyNoInteractions(walletService);
    }

    @Test
    void transferWithMatchingCustomerAndScopeReturnsOk()
            throws Exception {

        UUID customerId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        CurrencyTransferResponse response =
                new CurrencyTransferResponse(
                        transferId,
                        walletId,
                        "GHS",
                        "USD",
                        new BigDecimal("500.00"),
                        new BigDecimal("40.00"),
                        new BigDecimal("0.08"),
                        new BigDecimal("5.00"),
                        "COMPLETED",
                        "Test transfer",
                        Instant.now()
                );

        when(walletService.executeTransfer(
                any(CurrencyTransferRequest.class),
                eq(customerId)
        )).thenReturn(response);

        mockMvc.perform(
                post("/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(customerId))
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        customerId.toString()
                                )
                                .claim("scope", "wallet:write")
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transfer_id")
                .value(transferId.toString()))
        .andExpect(jsonPath("$.wallet_id")
                .value(walletId.toString()))
        .andExpect(jsonPath("$.from_currency")
                .value("GHS"))
        .andExpect(jsonPath("$.to_currency")
                .value("USD"))
        .andExpect(jsonPath("$.status")
                .value("COMPLETED"));

        verify(walletService).executeTransfer(
                any(CurrencyTransferRequest.class),
                eq(customerId)
        );
    }

    @Test
    void conversionQuoteRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                post("/v1/wallet/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "from_currency": "GHS",
                                  "to_currency": "USD",
                                  "amount": 100.00
                                }
                                """)
        )
        .andExpect(status().isUnauthorized());

        verifyNoInteractions(walletService);
    }

    @Test
    void conversionQuoteAuthenticatedUserCanAccess()
            throws Exception {

        CurrencyConvertResponse response =
                new CurrencyConvertResponse(
                        "GHS",
                        "USD",
                        new BigDecimal("100.00"),
                        new BigDecimal("8.00"),
                        new BigDecimal("0.08"),
                        new BigDecimal("12.50"),
                        new BigDecimal("1.50"),
                        new BigDecimal("1.00"),
                        "quote-123",
                        Instant.now()
                );

        when(walletService.getConversionQuote(
                any(CurrencyConvertRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                post("/v1/wallet/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "from_currency": "GHS",
                                  "to_currency": "USD",
                                  "amount": 100.00
                                }
                                """)
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        UUID.randomUUID().toString()
                                )
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.from_currency").value("GHS"))
        .andExpect(jsonPath("$.to_currency").value("USD"));

        verify(walletService)
                .getConversionQuote(any(CurrencyConvertRequest.class));
    }

    @Test
    void supportedCurrenciesRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                get("/v1/wallet/currencies")
        )
        .andExpect(status().isUnauthorized());

        verifyNoInteractions(walletService);
    }

    @Test
    void supportedCurrenciesAuthenticatedRequestReturnsOk()
            throws Exception {

        SupportedCurrenciesResponse response =
                new SupportedCurrenciesResponse(
                        "GHS",
                        List.of(),
                        List.of()
                );

        when(walletService.getSupportedCurrencies())
                .thenReturn(response);

        mockMvc.perform(
                get("/v1/wallet/currencies")
                        .with(jwt().jwt(builder -> builder
                                .claim("token_type", "user")
                                .claim("user_id", UUID.randomUUID().toString())
                                .claim(
                                        "customer_id",
                                        UUID.randomUUID().toString()
                                )
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.default_currency").value("GHS"));

        verify(walletService).getSupportedCurrencies();
    }

    private String transferJson(UUID customerId) {
        return """
                {
                  "customer_id": "%s",
                  "from_currency": "GHS",
                  "to_currency": "USD",
                  "amount": 500.00,
                  "reference": "Test transfer"
                }
                """.formatted(customerId);
    }
}