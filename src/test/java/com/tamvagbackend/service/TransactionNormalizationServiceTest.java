package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.service.connector.ConnectorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionNormalizationServiceTest {

    private TransactionNormalizationService service;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new TransactionNormalizationService();

        account = new Account();
        account.setAccountId(UUID.randomUUID());
    }

    @Test
    void shouldNormalizeProviderTransactionIntoCanonicalTransaction() {
        Instant occurredAt = Instant.parse("2026-09-15T10:15:30Z");

        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token-001",
                        "GCB-EVENT-001",
                        "in",
                        new BigDecimal("2500.0000"),
                        "ghs",
                        occurredAt,
                        " bank_current ",
                        " GCB PAYROLL ",
                        " salary ",
                        " salary payment ",
                        " gcb "
                );

        Transaction result = service.normalize(
                account,
                providerTransaction
        );

        assertNotNull(result.getTransactionId());
        assertSame(account, result.getAccount());

        assertEquals("GCB-EVENT-001", result.getSourceEventId());
        assertEquals("IN", result.getDirection());
        assertEquals(
                new BigDecimal("2.5E+3"),
                result.getAmount()
        );
        assertEquals("GHS", result.getCurrency());

        assertEquals(occurredAt, result.getOccurredAt());
        assertEquals("NORMALISED", result.getStatus());

        assertEquals("bank_current", result.getChannel());
        assertEquals("GCB PAYROLL", result.getCounterparty());
        assertEquals("salary", result.getMerchantCategory());
        assertEquals("salary payment", result.getReference());
        assertEquals("GCB", result.getSourceSystem());

        assertEquals(
                TransactionNormalizationService.NORMALISATION_VERSION,
                result.getNormalisationVersion()
        );

        assertNotNull(result.getCreatedAt());
    }

    @Test
    void shouldNormalizeOutgoingDirection() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token-002",
                        "MTN-EVENT-001",
                        "out",
                        new BigDecimal("125.00"),
                        "GHS",
                        Instant.parse("2026-09-15T11:00:00Z"),
                        "MOBILE_MONEY",
                        "MTN",
                        "SERVICE_FEE",
                        "service fee",
                        "MTN"
                );

        Transaction result = service.normalize(
                account,
                providerTransaction
        );

        assertEquals("OUT", result.getDirection());
        assertEquals(
                new BigDecimal("125"),
                result.getAmount()
        );
        assertEquals("SERVICE_FEE", result.getMerchantCategory());
        assertEquals("MTN", result.getSourceSystem());
    }

    @Test
    void shouldRejectNegativeAmount() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token-003",
                        "EVENT-NEGATIVE",
                        "OUT",
                        new BigDecimal("-75.50"),
                        "GHS",
                        Instant.now(),
                        "MOBILE_MONEY",
                        "Provider",
                        null,
                        "test",
                        "TEST"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, providerTransaction)
        );

        assertEquals(
                "Provider transaction amount cannot be negative",
                exception.getMessage()
        );
    }

    @Test
    void shouldDefaultCurrencyToGhsWhenMissing() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token-004",
                        "EVENT-CURRENCY",
                        "IN",
                        new BigDecimal("100"),
                        null,
                        Instant.now(),
                        "BANK",
                        "Provider",
                        null,
                        "test",
                        "TEST"
                );

        Transaction result = service.normalize(
                account,
                providerTransaction
        );

        assertEquals("GHS", result.getCurrency());
    }

    @Test
    void shouldDefaultOccurredAtWhenMissing() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token-005",
                        "EVENT-TIME",
                        "IN",
                        new BigDecimal("100"),
                        "GHS",
                        null,
                        "BANK",
                        "Provider",
                        null,
                        "test",
                        "TEST"
                );

        Instant before = Instant.now();

        Transaction result = service.normalize(
                account,
                providerTransaction
        );

        Instant after = Instant.now();

        assertNotNull(result.getOccurredAt());
        assertFalse(result.getOccurredAt().isBefore(before));
        assertFalse(result.getOccurredAt().isAfter(after));
    }

    @Test
    void shouldTrimOptionalFieldsAndConvertBlankValuesToNull() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token-006",
                        "EVENT-OPTIONAL",
                        "IN",
                        new BigDecimal("100"),
                        " GHS ",
                        Instant.now(),
                        " BANK ",
                        " Counterparty ",
                        " Category ",
                        " Reference ",
                        " Provider "
                );

        Transaction result = service.normalize(
                account,
                providerTransaction
        );

        assertEquals("BANK", result.getChannel());
        assertEquals("Counterparty", result.getCounterparty());
        assertEquals("Category", result.getMerchantCategory());
        assertEquals("Reference", result.getReference());
        assertEquals("PROVIDER", result.getSourceSystem());
    }

    @Test
    void shouldRejectMissingAccount() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                validProviderTransaction();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(null, providerTransaction)
        );

        assertEquals(
                "Account is required",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectMissingProviderTransaction() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, null)
        );

        assertEquals(
                "Provider transaction is required",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectMissingAccountReference() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        " ",
                        "EVENT-001",
                        "IN",
                        new BigDecimal("100"),
                        "GHS",
                        Instant.now(),
                        "BANK",
                        "Provider",
                        null,
                        "Reference",
                        "TEST"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, providerTransaction)
        );
    }

    @Test
    void shouldRejectMissingSourceEventId() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token",
                        " ",
                        "IN",
                        new BigDecimal("100"),
                        "GHS",
                        Instant.now(),
                        "BANK",
                        "Provider",
                        null,
                        "Reference",
                        "TEST"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, providerTransaction)
        );
    }

    @Test
    void shouldRejectInvalidDirection() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token",
                        "EVENT-001",
                        "TRANSFER",
                        new BigDecimal("100"),
                        "GHS",
                        Instant.now(),
                        "BANK",
                        "Provider",
                        null,
                        "Reference",
                        "TEST"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, providerTransaction)
        );

        assertEquals(
                "Provider transaction direction must be IN or OUT",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullAmount() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token",
                        "EVENT-001",
                        "IN",
                        null,
                        "GHS",
                        Instant.now(),
                        "BANK",
                        "Provider",
                        null,
                        "Reference",
                        "TEST"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, providerTransaction)
        );

        assertEquals(
                "Provider transaction amount is required",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectInvalidCurrencyLength() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "acct-token",
                        "EVENT-001",
                        "IN",
                        new BigDecimal("100"),
                        "GH",
                        Instant.now(),
                        "BANK",
                        "Provider",
                        null,
                        "Reference",
                        "TEST"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.normalize(account, providerTransaction)
        );

        assertEquals(
                "Provider transaction currency must be a 3-letter ISO code",
                exception.getMessage()
        );
    }

    @Test
    void shouldDefaultMissingSourceSystemToUnknown() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "ACC-001",
                        "evt-unknown-source",
                        "IN",
                        new BigDecimal("100.00"),
                        "GHS",
                        Instant.parse("2026-09-14T10:00:00Z"),
                        "BANK_CURRENT",
                        "EMPLOYER",
                        null,
                        "Salary",
                        null
                );

        Transaction result = service.normalize(
                account,
                providerTransaction
        );

        assertEquals("UNKNOWN", result.getSourceSystem());
    }

    private ConnectorProvider.ProviderTransaction validProviderTransaction() {
        return new ConnectorProvider.ProviderTransaction(
                "acct-token",
                "EVENT-001",
                "IN",
                new BigDecimal("100"),
                "GHS",
                Instant.now(),
                "BANK",
                "Provider",
                null,
                "Reference",
                "TEST"
        );
    }
}
