package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MultiCurrencyDtos {

    public record CurrencyMetadata(
            String code,
            String name,
            String symbol,
            String flagEmoji,
            boolean isDefault
    ) {}

    public record SupportedCurrenciesResponse(
            @JsonProperty("default_currency")
            String defaultCurrency,

            @JsonProperty("supported_currencies")
            List<CurrencyMetadata> supportedCurrencies,

            @JsonProperty("base_rates_against_ghs")
            List<ExchangeRateDetail> ratesAgainstGhs
    ) {}

    public record ExchangeRateDetail(
            @JsonProperty("base_currency")
            String baseCurrency,

            @JsonProperty("quote_currency")
            String quoteCurrency,

            BigDecimal rate,

            @JsonProperty("inverse_rate")
            BigDecimal inverseRate,

            @JsonProperty("spread_percentage")
            BigDecimal spreadPercentage,

            @JsonProperty("effective_at")
            Instant effectiveAt
    ) {}

    public record CurrencyConvertRequest(
            @NotBlank(message = "from_currency is required")
            @JsonProperty("from_currency")
            @Schema(example = "GHS")
            String fromCurrency,

            @NotBlank(message = "to_currency is required")
            @JsonProperty("to_currency")
            @Schema(example = "USD")
            String toCurrency,

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
            @Schema(example = "1000.00")
            BigDecimal amount
    ) {}

    public record CurrencyConvertResponse(
            @JsonProperty("from_currency")
            @Schema(example = "GHS")
            String fromCurrency,

            @JsonProperty("to_currency")
            @Schema(example = "USD")
            String toCurrency,

            @JsonProperty("from_amount")
            BigDecimal fromAmount,

            @JsonProperty("to_amount")
            BigDecimal toAmount,

            @JsonProperty("exchange_rate")
            BigDecimal exchangeRate,

            @JsonProperty("inverse_rate")
            BigDecimal inverseRate,

            @JsonProperty("spread_percentage")
            BigDecimal spreadPercentage,

            @JsonProperty("fee_amount")
            BigDecimal feeAmount,

            @JsonProperty("quote_id")
            String quoteId,

            @JsonProperty("effective_at")
            Instant effectiveAt
    ) {}

    public record CurrencyTransferRequest(
            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @NotBlank(message = "from_currency is required")
            @JsonProperty("from_currency")
            @Schema(example = "GHS")
            String fromCurrency,

            @NotBlank(message = "to_currency is required")
            @JsonProperty("to_currency")
            @Schema(example = "USD")
            String toCurrency,

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            @Schema(example = "500.00")
            BigDecimal amount,

            @Schema(example = "Swagger integration test transfer")
            String reference
    ) {}

    public record CurrencyTransferResponse(
            @JsonProperty("transfer_id")
            UUID transferId,

            @JsonProperty("wallet_id")
            UUID walletId,

            @JsonProperty("from_currency")
            @Schema(example = "GHS")
            String fromCurrency,

            @JsonProperty("to_currency")
            @Schema(example = "USD")
            String toCurrency,

            @JsonProperty("from_amount")
            BigDecimal fromAmount,

            @JsonProperty("to_amount")
            BigDecimal toAmount,

            @JsonProperty("rate_applied")
            BigDecimal rateApplied,

            @JsonProperty("fee_amount")
            BigDecimal feeAmount,

            String status,
            String reference,

            @JsonProperty("created_at")
            Instant createdAt
    ) {}

    public record CurrencyBalanceItem(
            String currency,
            String symbol,
            String flagEmoji,

            @JsonProperty("available_amount")
            BigDecimal availableAmount,

            @JsonProperty("locked_amount")
            BigDecimal lockedAmount,

            @JsonProperty("converted_to_display_currency")
            BigDecimal convertedToDisplayCurrency
    ) {}

    public record MultiCurrencyWalletResponse(
            @JsonProperty("wallet_id")
            UUID walletId,

            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @JsonProperty("default_currency")
            String defaultCurrency,

            @JsonProperty("display_currency")
            String displayCurrency,

            @JsonProperty("total_portfolio_value")
            BigDecimal totalPortfolioValue,

            List<CurrencyBalanceItem> balances,

            @JsonProperty("recent_transfers")
            List<CurrencyTransferResponse> recentTransfers
    ) {}
}
