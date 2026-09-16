package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public final class FeatureDtos {

    private FeatureDtos() {
    }

    public record ComputeFeatureRequest(
            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("period_start")
            LocalDate periodStart,

            @JsonProperty("period_end")
            LocalDate periodEnd
    ) {
    }

    public record FeatureSnapshotResponse(
            @JsonProperty("feature_snapshot_id")
            UUID featureSnapshotId,

            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("period_start")
            LocalDate periodStart,

            @JsonProperty("period_end")
            LocalDate periodEnd,

            @JsonProperty("feature_version")
            String featureVersion,

            @JsonProperty("total_inflows")
            BigDecimal totalInflows,

            @JsonProperty("total_outflows")
            BigDecimal totalOutflows,

            @JsonProperty("net_cash_flow")
            BigDecimal netCashFlow,

            @JsonProperty("transaction_count")
            long transactionCount,

            @JsonProperty("inflow_transaction_count")
            long inflowTransactionCount,

            @JsonProperty("outflow_transaction_count")
            long outflowTransactionCount,

            @JsonProperty("average_inflow")
            BigDecimal averageInflow,

            @JsonProperty("average_outflow")
            BigDecimal averageOutflow,

            @JsonProperty("largest_inflow")
            BigDecimal largestInflow,

            @JsonProperty("largest_outflow")
            BigDecimal largestOutflow,

            @JsonProperty("income_consistency")
            BigDecimal incomeConsistency,

            @JsonProperty("expense_consistency")
            BigDecimal expenseConsistency,

            @JsonProperty("computed_at")
            Instant computedAt
    ) {
    }
}