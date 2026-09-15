package com.tamvagbackend.service.connector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class TelecelConnectorProvider implements ConnectorProvider {

    @Override
    public String getProviderCode() {
        return "TELECEL";
    }

    @Override
    public List<ProviderTransaction> fetchTransactions(
            ConnectorSyncContext context
    ) {
        /*
         * Development/pilot adapter.
         *
         * This adapter intentionally does not call a real Telecel API.
         * It provides deterministic provider-shaped transactions so
         * the connector -> normalization -> ledger pipeline can be
         * exercised end-to-end.
         */
        String accountRefToken = context.providerRef();

        if (accountRefToken == null || accountRefToken.isBlank()) {
            return List.of();
        }

        Instant occurredAt = context.lastSyncAt() != null
                ? context.lastSyncAt().plusSeconds(60)
                : Instant.now().minusSeconds(3600);

        return List.of(
                new ProviderTransaction(
                        accountRefToken,
                        "TELECEL-" + context.connectionId() + "-PILOT-001",
                        "IN",
                        new BigDecimal("1200.00"),
                        "GHS",
                        occurredAt,
                        "MOBILE_MONEY",
                        "TELECEL MOBILE MONEY",
                        null,
                        "CASH_IN",
                        "TELECEL"
                ),
                new ProviderTransaction(
                        accountRefToken,
                        "TELECEL-" + context.connectionId() + "-PILOT-002",
                        "OUT",
                        new BigDecimal("50.00"),
                        "GHS",
                        occurredAt.plusSeconds(60),
                        "MOBILE_MONEY",
                        "TELECEL",
                        "SERVICE_FEE",
                        "Telecel mobile money service fee",
                        "TELECEL"
                )
        );
    }
}
