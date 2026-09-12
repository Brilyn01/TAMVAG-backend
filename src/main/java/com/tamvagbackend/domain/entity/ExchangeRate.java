package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exchange_rate", uniqueConstraints = {
    @UniqueConstraint(name = "uq_base_quote", columnNames = {"base_currency", "quote_currency"})
})
public class ExchangeRate {

    @Id
    @Column(name = "rate_id", nullable = false)
    private UUID rateId;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(name = "rate", nullable = false, precision = 20, scale = 6)
    private BigDecimal rate;

    @Column(name = "inverse_rate", nullable = false, precision = 20, scale = 6)
    private BigDecimal inverseRate;

    @Column(name = "spread_percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal spreadPercentage = new BigDecimal("0.0050");

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt = Instant.now();

    public ExchangeRate() {
        this.rateId = UUID.randomUUID();
    }

    public ExchangeRate(String baseCurrency, String quoteCurrency, BigDecimal rate, BigDecimal inverseRate, BigDecimal spreadPercentage) {
        this.rateId = UUID.randomUUID();
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.inverseRate = inverseRate;
        this.spreadPercentage = spreadPercentage != null ? spreadPercentage : new BigDecimal("0.0050");
        this.effectiveAt = Instant.now();
    }

    public UUID getRateId() { return rateId; }
    public void setRateId(UUID rateId) { this.rateId = rateId; }

    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }

    public String getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getInverseRate() { return inverseRate; }
    public void setInverseRate(BigDecimal inverseRate) { this.inverseRate = inverseRate; }

    public BigDecimal getSpreadPercentage() { return spreadPercentage; }
    public void setSpreadPercentage(BigDecimal spreadPercentage) { this.spreadPercentage = spreadPercentage; }

    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
}
