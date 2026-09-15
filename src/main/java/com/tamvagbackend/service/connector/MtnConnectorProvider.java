package com.tamvagbackend.service.connector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class MtnConnectorProvider implements ConnectorProvider {

    @Override
    public String getProviderCode() {
        return "MTN";
    }

    @Override
    public List<ProviderTransaction> fetchTransactions(
            ConnectorSyncContext context
    ) {
        /*
         * Development/pilot adapter.
         *
         * This adapter intentionally does not call a real MTN API.
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
                        "MTN-" + context.connectionId() + "-PILOT-001",
                        "IN",
                        new BigDecimal("1800.00"),
                        "GHS",
                        occurredAt,
                        "MOBILE_MONEY",
                        "MTN MOBILE MONEY",
                        null,
                        "SALARY",
                        "MTN"
                ),
                new ProviderTransaction(
                        accountRefToken,
                        "MTN-" + context.connectionId() + "-PILOT-002",
                        "OUT",
                        new BigDecimal("75.00"),
                        "GHS",
                        occurredAt.plusSeconds(60),
                        "MOBILE_MONEY",
                        "MTN",
                        "SERVICE_FEE",
                        "MTN mobile money service fee",
                        "MTN"
                )
        );
    }
}
