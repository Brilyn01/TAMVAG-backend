package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.service.connector.ConnectorProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransactionNormalizationService {

    /**
     * Canonical transaction normalization version.
     *
     * Increment this value whenever the normalization rules change
     * in a way that can alter the canonical representation.
     */
    public static final String NORMALISATION_VERSION = "1.0.0";

    /**
     * Converts a provider transaction into TAMVA's canonical Transaction
     * representation.
     *
     * This service is deliberately responsible only for normalization.
     * Persistence, idempotency and ledger creation remain outside it.
     */
    public Transaction normalize(
            Account account,
            ConnectorProvider.ProviderTransaction providerTransaction
    ) {
        if (account == null) {
            throw new IllegalArgumentException("Account is required");
        }

        if (providerTransaction == null) {
            throw new IllegalArgumentException("Provider transaction is required");
        }

        validate(providerTransaction);

        Transaction transaction = new Transaction();

        transaction.setTransactionId(UUID.randomUUID());
        transaction.setAccount(account);

        transaction.setSourceEventId(
                normalizeRequired(providerTransaction.sourceEventId())
        );

        transaction.setDirection(
                normalizeDirection(providerTransaction.direction())
        );

        transaction.setAmount(
                normalizeAmount(providerTransaction.amount())
        );

        transaction.setCurrency(
                normalizeCurrency(providerTransaction.currency())
        );

        transaction.setOccurredAt(
                providerTransaction.occurredAt() != null
                        ? providerTransaction.occurredAt()
                        : Instant.now()
        );

        transaction.setStatus("NORMALISED");

        transaction.setChannel(
                normalizeOptional(providerTransaction.channel())
        );

        transaction.setCounterparty(
                normalizeOptional(providerTransaction.counterparty())
        );

        transaction.setMerchantCategory(
                normalizeOptional(providerTransaction.merchantCategory())
        );

        transaction.setReference(
                normalizeOptional(providerTransaction.reference())
        );

        transaction.setSourceSystem(
                normalizeOptional(providerTransaction.sourceSystem())
        );

        transaction.setNormalisationVersion(NORMALISATION_VERSION);
        transaction.setCreatedAt(Instant.now());

        return transaction;
    }

    private void validate(
            ConnectorProvider.ProviderTransaction providerTransaction
    ) {
        if (providerTransaction.accountRefToken() == null
                || providerTransaction.accountRefToken().isBlank()) {
            throw new IllegalArgumentException(
                    "Provider transaction account reference is required"
            );
        }

        if (providerTransaction.sourceEventId() == null
                || providerTransaction.sourceEventId().isBlank()) {
            throw new IllegalArgumentException(
                    "Provider transaction source event ID is required"
            );
        }

        if (providerTransaction.direction() == null
                || providerTransaction.direction().isBlank()) {
            throw new IllegalArgumentException(
                    "Provider transaction direction is required"
            );
        }

        if (!"IN".equalsIgnoreCase(providerTransaction.direction())
                && !"OUT".equalsIgnoreCase(providerTransaction.direction())) {
            throw new IllegalArgumentException(
                    "Provider transaction direction must be IN or OUT"
            );
        }

        if (providerTransaction.amount() == null) {
            throw new IllegalArgumentException(
                    "Provider transaction amount is required"
            );
        }

        if (providerTransaction.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Provider transaction amount cannot be negative"
            );
        }

        if (providerTransaction.currency() != null
                && !providerTransaction.currency().isBlank()
                && providerTransaction.currency().trim().length() != 3) {
            throw new IllegalArgumentException(
                    "Provider transaction currency must be a 3-letter ISO code"
            );
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required value is missing");
        }

        return value.trim();
    }

    private String normalizeDirection(String direction) {
        return normalizeRequired(direction).toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        return amount.abs().stripTrailingZeros();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "GHS";
        }

        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
