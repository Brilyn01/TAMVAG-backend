package com.tamvagbackend.service.connector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class GcbConnectorProvider implements ConnectorProvider {

    @Override
    public String getProviderCode() {
        return "GCB";
    }

    @Override
    public List<ProviderTransaction> fetchTransactions(
            ConnectorSyncContext context
    ) {
        /*
         * Development/pilot adapter.
         *
         * This intentionally does not call a real GCB API.
         * It provides deterministic provider-shaped data so that the
         * connector pipeline can be exercised end-to-end.
         *
         * A production GCB implementation can replace this method with
         * the authenticated GCB API integration without changing
         * ConnectorService or the canonical transaction contract.
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
                        "GCB-" + context.connectionId() + "-PILOT-001",
                        "IN",
                        new BigDecimal("2500.00"),
                        "GHS",
                        occurredAt,
                        "BANK_CURRENT",
                        "GCB PILOT PAYROLL",
                        "SALARY",
                        "GCB payroll credit",
                        "GCB"
                ),
                new ProviderTransaction(
                        accountRefToken,
                        "GCB-" + context.connectionId() + "-PILOT-002",
                        "OUT",
                        new BigDecimal("125.00"),
                        "GHS",
                        occurredAt.plusSeconds(60),
                        "BANK_CURRENT",
                        "GCB",
                        "FEE",
                        "GCB account service fee",
                        "GCB"
                )
        );
    }
}