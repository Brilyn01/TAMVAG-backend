package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import com.tamvagbackend.dto.MultiCurrencyDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MultiCurrencyWalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletBalanceRepository walletBalanceRepository;
    @Mock private ExchangeRateRepository exchangeRateRepository;
    @Mock private CurrencyTransferRepository currencyTransferRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private MultiCurrencyWalletService walletService;

    @Test
    void testGetSupportedCurrencies_Contains8CurrenciesWithGhsDefault() {
        SupportedCurrenciesResponse response = walletService.getSupportedCurrencies();
        assertNotNull(response);
        assertEquals("GHS", response.defaultCurrency());
        assertEquals(8, response.supportedCurrencies().size());

        List<String> codes = response.supportedCurrencies().stream().map(CurrencyMetadata::code).toList();
        assertTrue(codes.containsAll(List.of("GHS", "NGN", "KES", "ZAR", "EGP", "USD", "GBP", "EUR")));
    }

    @Test
    void testGetConversionQuote_GhsToUsd() {
        CurrencyConvertRequest req = new CurrencyConvertRequest("GHS", "USD", new BigDecimal("100.00"));
        CurrencyConvertResponse quote = walletService.getConversionQuote(req);

        assertNotNull(quote);
        assertEquals("GHS", quote.fromCurrency());
        assertEquals("USD", quote.toCurrency());
        assertEquals(new BigDecimal("100.00"), quote.fromAmount());
        assertTrue(quote.toAmount().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(quote.quoteId());
    }

    @Test
    void testCalculateCrossRate_GhsToNgn() {
        BigDecimal rate = walletService.calculateCrossRate("GHS", "NGN");
        assertNotNull(rate);
        assertTrue(rate.compareTo(new BigDecimal("100.00")) > 0);
    }
}
