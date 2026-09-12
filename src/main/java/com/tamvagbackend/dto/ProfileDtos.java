package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProfileDtos {

    public record IncomeSummary(
            @JsonProperty("monthly_estimate")
            BigDecimal monthlyEstimate,

            BigDecimal consistency,
            BigDecimal confidence
    ) {}

    public record CashFlowSummary(
            @JsonProperty("avg_inflow_90d")
            BigDecimal avgInflow90d,

            @JsonProperty("avg_outflow_90d")
            BigDecimal avgOutflow90d,

            @JsonProperty("net_90d")
            BigDecimal net90d,

            BigDecimal volatility
    ) {}

    public record SavingsSummary(
            BigDecimal consistency,

            @JsonProperty("savings_rate_90d")
            BigDecimal savingsRate90d,

            @JsonProperty("estimated_savings_balance")
            BigDecimal estimatedSavingsBalance
    ) {}

    public record DebtSummary(
            String pressure, // LOW, MODERATE, HIGH, SEVERE

            @JsonProperty("debt_service_ratio_30d")
            BigDecimal debtServiceRatio30d,

            @JsonProperty("repayment_consistency")
            BigDecimal repaymentConsistency
    ) {}

    public record CustomerProfileResponse(
            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("as_of")
            Instant asOf,

            IncomeSummary income,

            @JsonProperty("cash_flow")
            CashFlowSummary cashFlow,

            SavingsSummary savings,
            DebtSummary debt,

            @JsonProperty("confidence_score")
            int confidenceScore,

            @JsonProperty("evidence_window_days")
            int evidenceWindowDays,

            @JsonProperty("profile_version")
            String profileVersion,

            @JsonProperty("connected_accounts_count")
            int connectedAccountsCount,

            @JsonProperty("total_transactions_analyzed")
            int totalTransactionsAnalyzed
    ) {}
}
