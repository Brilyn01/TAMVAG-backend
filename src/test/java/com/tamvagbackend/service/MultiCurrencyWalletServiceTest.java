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

    @Test
    void testExecuteTransfer_IdempotentDuplicateReference() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        Wallet wallet = new Wallet(customer, "GHS");
        wallet.setWalletId(UUID.randomUUID());

        CurrencyTransfer existingTransfer = new CurrencyTransfer();
        existingTransfer.setTransferId(UUID.randomUUID());
        existingTransfer.setWallet(wallet);
        existingTransfer.setFromCurrency("GHS");
        existingTransfer.setToCurrency("USD");
        existingTransfer.setFromAmount(new BigDecimal("100.00"));
        existingTransfer.setToAmount(new BigDecimal("6.50"));
        existingTransfer.setRateApplied(new BigDecimal("0.065000"));
        existingTransfer.setFeeAmount(new BigDecimal("0.50"));
        existingTransfer.setStatus("COMPLETED");
        existingTransfer.setReference("TXF_DUP_001");
        existingTransfer.setCreatedAt(java.time.Instant.now());

        org.mockito.Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        org.mockito.Mockito.when(walletRepository.findByCustomer(customer)).thenReturn(Optional.of(wallet));
        org.mockito.Mockito.when(currencyTransferRepository.findByWalletAndReference(wallet, "TXF_DUP_001"))
                .thenReturn(Optional.of(existingTransfer));

        CurrencyTransferRequest req = new CurrencyTransferRequest(customerId, "GHS", "USD", new BigDecimal("100.00"), "TXF_DUP_001");
        CurrencyTransferResponse response = walletService.executeTransfer(req);

        assertNotNull(response);
        assertEquals("TXF_DUP_001", response.reference());
        assertEquals(existingTransfer.getTransferId(), response.transferId());

        org.mockito.Mockito.verify(walletBalanceRepository, org.mockito.Mockito.never()).save(any());
    }
}

