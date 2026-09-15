package com.tamvagbackend.service.connector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConnectorProvider {

    String getProviderCode();

    List<ProviderTransaction> fetchTransactions(
            ConnectorSyncContext context
    );

    record ProviderTransaction(
            String accountRefToken,
            String sourceEventId,
            String direction,
            BigDecimal amount,
            String currency,
            Instant occurredAt,
            String channel,
            String counterparty,
            String merchantCategory,
            String reference,
            String sourceSystem
    ) {}

    record ConnectorSyncContext(
            UUID connectionId,
            UUID customerId,
            UUID institutionId,
            String providerRef,
            String syncMode,
            Instant lastSyncAt
    ) {}
}